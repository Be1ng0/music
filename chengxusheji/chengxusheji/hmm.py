import os
from openpyxl import load_workbook
import pandas as pd
import numpy as np
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.metrics import classification_report
import matplotlib.pyplot as plt
import xlrd
import xlwt
import math


class HybridModel:
    def __init__(self, hmm_config_path, emotion_weight=0.5):
        """
        初始化混合模型
        :param hmm_config_path: HMM配置文件的路径前缀
        :param emotion_weight: 情感权重(0-1之间，0表示完全悲伤，1表示完全快乐)
        """
        self.hmm_config_path = hmm_config_path
        self.emotion_weight = emotion_weight
        self.nn_model = None
        self.hmm = None
        self.label_encoder = None
        self.max_obs_state = None  # 跟踪最大观测状态

    def read_excel_auto(self, file_path):
        """专用于读取.xls文件的可靠方法"""
        try:
            # 确保使用绝对路径
            file_path = os.path.abspath(file_path)

            # 方法1：使用xlrd（最佳选择）
            import xlrd
            book = xlrd.open_workbook(file_path)
            sheet = book.sheet_by_index(0)
            return sheet.row_values(0)

        except Exception as e:
            # 方法2：使用pandas备用方案
            try:
                import pandas as pd
                df = pd.read_excel(file_path, engine='xlrd')
                return df.iloc[0].tolist()
            except Exception as e:
                raise ValueError(f"无法读取.xls文件: {str(e)}\n"
                                 f"文件路径: {file_path}\n"
                                 f"文件大小: {os.path.getsize(file_path)}字节")

    def load_data(self, csv_path, augment=False):
        """加载并预处理数据"""
        df = pd.read_csv(csv_path)
        df = df.drop(['filename'], axis=1)  # 假设存在filename列
        df = df.dropna()

        X = df.drop('label', axis=1).values
        y = df['label'].values

        # 标签编码
        self.label_encoder = LabelEncoder()
        y = self.label_encoder.fit_transform(y)

        # 数据标准化
        scaler = StandardScaler()
        X = scaler.fit_transform(X)

        # 数据增强
        if augment:
            noise_factor = 0.05
            X_noised = X + noise_factor * np.random.normal(size=X.shape)
            X = np.vstack([X, X_noised])
            y = np.hstack([y, y])

        return X, y

    def build_nn_model(self, input_shape, num_classes):
        """构建神经网络模型"""
        model = tf.keras.Sequential([
            tf.keras.layers.Dense(128, activation='relu',
                                  kernel_regularizer=tf.keras.regularizers.l2(0.001),
                                  input_shape=(input_shape,)),
            tf.keras.layers.BatchNormalization(),
            tf.keras.layers.Dropout(0.5),

            tf.keras.layers.Dense(64, activation='relu',
                                  kernel_regularizer=tf.keras.regularizers.l2(0.001)),
            tf.keras.layers.BatchNormalization(),
            tf.keras.layers.Dropout(0.3),

            tf.keras.layers.Dense(num_classes, activation='softmax')
        ])

        optimizer = tf.keras.optimizers.Adam(learning_rate=0.0005)
        model.compile(optimizer=optimizer,
                      loss='sparse_categorical_crossentropy',
                      metrics=['accuracy'])
        return model

    def train_nn(self, X_train, y_train, X_val, y_val, epochs=50, batch_size=64):
        """训练神经网络"""
        callbacks = [
            tf.keras.callbacks.EarlyStopping(patience=10,
                                             restore_best_weights=True,
                                             monitor='val_loss'),
            tf.keras.callbacks.ReduceLROnPlateau(factor=0.5,
                                                 patience=3,
                                                 verbose=1)
        ]

        history = self.nn_model.fit(X_train, y_train,
                                    epochs=epochs,
                                    batch_size=batch_size,
                                    validation_data=(X_val, y_val),
                                    callbacks=callbacks,
                                    verbose=1)
        return history

    def initialize_hmm(self):
        """初始化HMM模型"""
        # 读取HMM参数并转换为NumPy数组
        pi = np.array(self.read_data_single(f'{self.hmm_config_path}开头和弦-pi.xls'), dtype=np.float64)
        A_happy = np.array(self.read_data_mul(f'{self.hmm_config_path}大调和弦矩阵-a.xls'), dtype=np.float64)
        A_sad = np.array(self.read_data_mul(f'{self.hmm_config_path}小调和弦矩阵-a.xls'), dtype=np.float64)

        # 组合转移概率矩阵
        A = np.zeros((60, 60))
        for i in range(60):
            for j in range(60):
                if A_happy[i][j] == 0:
                    A_happy[i][j] = 0.0001
                if A_sad[i][j] == 0:
                    A_sad[i][j] = 0.0001
                temp = self.emotion_weight * math.log10(A_happy[i][j]) + \
                       (1 - self.emotion_weight) * math.log10(A_sad[i][j])
                A[i][j] = 10 ** temp

        # 读取发射概率矩阵
        s_list1 = np.array(self.read_data_mul(f'{self.hmm_config_path}大调单音矩阵-b-pre.xls'), dtype=np.float64)
        for i in range(60):
            for j in range(12):
                if s_list1[i][j] == 0:
                    s_list1[i][j] = 0.001
                s_list1[i][j] = math.log10(s_list1[i][j])

        s_list2 = np.array(self.read_data_mul(f'{self.hmm_config_path}小调单音矩阵-b-pre.xls'), dtype=np.float64)
        for i in range(60):
            for j in range(12):
                if s_list2[i][j] == 0:
                    s_list2[i][j] = 0.001
                s_list2[i][j] = math.log10(s_list2[i][j])

        # 读取旋律数据
        melody = xlrd.open_workbook(f'{self.hmm_config_path}melody.xls')
        m_table = melody.sheets()[0]
        n_treasure = m_table.nrows
        m_list = []
        for i in range(n_treasure):
            m_list.append(m_table.row_values(i))
        mT = np.array(list(map(list, zip(*m_list))), dtype=np.float64)

        # 计算发射概率
        B1 = np.dot(s_list1, mT)
        for i in range(60):
            for j in range(n_treasure):
                B1[i][j] = 10 ** B1[i][j]

        sum_list = []
        for i in range(60):
            sum_val = 0
            for j in range(n_treasure):
                sum_val += B1[i][j]
            sum_list.append(sum_val)

        for i in range(60):
            for j in range(n_treasure):
                B1[i][j] = B1[i][j] / sum_list[i]

        B2 = np.dot(s_list2, mT)
        for i in range(60):
            for j in range(n_treasure):
                B2[i][j] = 10 ** B2[i][j]

        sum_list = []
        for i in range(60):
            sum_val = 0
            for j in range(n_treasure):
                sum_val += B2[i][j]
            sum_list.append(sum_val)

        for i in range(60):
            for j in range(n_treasure):
                B2[i][j] = B2[i][j] / sum_list[i]

        # 组合发射概率矩阵
        B = np.zeros((60, n_treasure))
        for i in range(60):
            for j in range(n_treasure):
                temp = self.emotion_weight * math.log10(B1[i][j]) + \
                       (1 - self.emotion_weight) * math.log10(B2[i][j])
                B[i][j] = 10 ** temp

        # 设置最大观测状态
        self.max_obs_state = n_treasure - 1

        # 创建HMM模型
        self.hmm = HMM(A, B, pi)

    def validate_hmm_params(self, A, B, pi):
        """验证HMM参数的有效性"""
        # 确保所有参数都是NumPy数组
        A = np.array(A, dtype=np.float64)
        B = np.array(B, dtype=np.float64)
        pi = np.array(pi, dtype=np.float64)

        # 验证形状
        assert A.shape == (60, 60), f"A矩阵形状错误: {A.shape}"
        assert B.shape[0] == 60, f"B矩阵行数错误: {B.shape[0]}"
        assert len(pi) == 60, f"pi向量长度错误: {len(pi)}"

        # 检查概率是否有效
        assert np.all(A >= 0), "A矩阵包含负值"
        assert np.all(B >= 0), "B矩阵包含负值"
        assert np.all(pi >= 0), "pi向量包含负值"

        # 检查概率是否合理（不要求严格归一化，因为可能有数值误差）
        assert np.all(np.isfinite(A)), "A矩阵包含非有限值"
        assert np.all(np.isfinite(B)), "B矩阵包含非有限值"
        assert np.all(np.isfinite(pi)), "pi向量包含非有限值"

        print(f"HMM参数验证通过: 状态数=60, 观测数={B.shape[1]}")

    def read_data_mul(self, file):
        """读取多维数据并转换为NumPy数组"""
        data = xlrd.open_workbook(file)
        table = data.sheets()[0]
        nrows = table.nrows
        list_data = []
        for i in range(nrows):
            list_data.append(table.row_values(i))
        return np.array(list_data, dtype=np.float64)

    def read_data_single(self, file):
        full_path = os.path.join(self.hmm_config_path or ".", file)
        print(f"正在读取文件: {full_path}")  # 调试信息
        data = self.read_excel_auto(full_path)
        return np.array(data, dtype=np.float64)

    def predict_sequence(self, X):
        """
        使用混合模型进行序列预测
        1. 使用神经网络获取初步预测
        2. 使用HMM进行序列优化
        """
        # 使用神经网络获取初步预测概率
        nn_probs = self.nn_model.predict(X)

        # 转换为观察序列
        O = [np.argmax(prob) for prob in nn_probs]

        # 确保观测值在有效范围内
        if self.max_obs_state is not None:
            O = [min(obs, self.max_obs_state) for obs in O]

        # 状态序列
        state_s = [i for i in range(60)]

        # 使用HMM进行序列优化
        optimized_sequence = self.hmm.viterbi(O, state_s)

        return optimized_sequence

    def train_and_evaluate(self, csv_path):
        """完整的训练和评估流程"""
        # 加载数据
        X, y = self.load_data(csv_path, augment=True)

        # 分割数据
        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
        X_train, X_val, y_train, y_val = train_test_split(X_train, y_train, test_size=0.2, random_state=42)

        # 构建并训练神经网络
        self.nn_model = self.build_nn_model(X.shape[1], len(np.unique(y)))
        history = self.train_nn(X_train, y_train, X_val, y_val)

        # 初始化HMM
        self.initialize_hmm()

        # 评估神经网络
        test_loss, test_acc = self.nn_model.evaluate(X_test, y_test, verbose=0)
        print(f"\nNeural Network Test accuracy: {test_acc:.4f}")

        # 生成神经网络分类报告
        y_pred_nn = np.argmax(self.nn_model.predict(X_test), axis=1)
        print("\nNeural Network Classification Report:")
        print(classification_report(y_test, y_pred_nn, target_names=self.label_encoder.classes_))

        # 使用混合模型进行序列预测
        print("\nUsing Hybrid Model for sequence prediction...")
        optimized_sequence = self.predict_sequence(X_test[:10])  # 只对前10个样本进行序列预测演示

        # 可视化训练过程
        self.plot_training_history(history)

        return optimized_sequence

    def plot_training_history(self, history):
        """绘制训练过程曲线"""
        plt.figure(figsize=(12, 5))
        plt.subplot(1, 2, 1)
        plt.plot(history.history['accuracy'], label='Train Accuracy')
        plt.plot(history.history['val_accuracy'], label='Val Accuracy')
        plt.title('Training Curves')
        plt.xlabel('Epoch')
        plt.ylabel('Accuracy')
        plt.legend()

        plt.subplot(1, 2, 2)
        plt.plot(history.history['loss'], label='Train Loss')
        plt.plot(history.history['val_loss'], label='Val Loss')
        plt.title('Loss Curves')
        plt.xlabel('Epoch')
        plt.ylabel('Loss')
        plt.legend()
        plt.tight_layout()
        plt.show()


class HMM:
    def __init__(self, A, B, pi):
        """
        A: 状态转移概率矩阵
        B: 输出观察概率矩阵
        pi: 初始化状态向量
        """
        # 确保所有参数都是NumPy数组
        self.A = np.array(A, dtype=np.float64)
        self.B = np.array(B, dtype=np.float64)
        self.pi = np.array(pi, dtype=np.float64)
        self.N = self.A.shape[0]  # 状态数量
        self.M = self.B.shape[1]  # 观察值数量

        # 验证参数
        self.validate_parameters()

    def validate_parameters(self):
        """验证HMM参数的有效性"""
        assert self.A.shape == (self.N, self.N), "A矩阵形状不正确"
        assert self.B.shape[0] == self.N, "B矩阵行数不正确"
        assert len(self.pi) == self.N, "pi向量长度不正确"

        # 检查概率是否有效
        assert np.all(self.A >= 0), "A矩阵包含负值"
        assert np.all(self.B >= 0), "B矩阵包含负值"
        assert np.all(self.pi >= 0), "pi向量包含负值"

        # 检查概率是否合理（不要求严格归一化，因为可能有数值误差）
        assert np.all(np.isfinite(self.A)), "A矩阵包含非有限值"
        assert np.all(np.isfinite(self.B)), "B矩阵包含非有限值"
        assert np.all(np.isfinite(self.pi)), "pi向量包含非有限值"

    def viterbi(self, obser, state):
        """维特比算法进行序列解码"""
        # 预处理观测序列
        obser = [min(obs, self.M - 1) for obs in obser]  # 确保观测值不越界

        # 初始化
        max_p = [[0 for _ in range(self.N)] for _ in range(len(obser))]
        path = [[0 for _ in range(self.N)] for _ in range(len(obser))]

        # 初始状态
        for i in range(self.N):
            max_p[0][i] = self.pi[i] * self.B[i][obser[0]]
            path[0][i] = i

        # 递推
        for t in range(1, len(obser)):
            max_item = [0 for _ in range(self.N)]
            for j in range(self.N):
                item = [0 for _ in state]
                for k in range(self.N):
                    # 添加边界检查
                    obs_idx = min(obser[t], self.M - 1)
                    p = max_p[t - 1][k] * self.B[j][obs_idx] * self.A[k][j]
                    item[state[k]] = p
                max_item[state[j]] = max(item)
                path[t][state[j]] = item.index(max(item))
            max_p[t] = max_item

        # 回溯
        best_path = []
        p = max_p[-1].index(max(max_p[-1]))
        best_path.append(p)

        for t in range(len(obser) - 1, 0, -1):
            best_path.append(path[t][p])
            p = path[t][p]

        best_path.reverse()
        return best_path


# 使用示例
if __name__ == "__main__":
    # 初始化混合模型
    # 第一个参数是HMM配置文件路径前缀，第二个参数是情感权重(0-1)
    hybrid_model = HybridModel(
        hmm_config_path="./",  # 表示当前目录
        emotion_weight=0.7
    )

    # 训练并评估模型
    optimized_sequence = hybrid_model.train_and_evaluate(
        r"C:\Users\29189\OneDrive\Desktop\chengxusheji\chengxusheji\data.csv"
    )

    print("\nOptimized sequence from hybrid model:", optimized_sequence)

    # 保存模型
    hybrid_model.nn_model.save("hybrid_model_nn_part.h5")
    # HMM部分不需要保存，因为每次使用时都会根据配置文件重新初始化