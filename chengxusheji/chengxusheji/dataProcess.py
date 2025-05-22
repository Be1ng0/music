import numpy

key = {'C': 0, 'bD': 1, 'D': 2, 'bE': 3, 'E': 4, 'F': 5, 'bG': 6, 'G': 7,
       'bA': 8, 'A': 9, 'bB': 10, 'B': 11}


def str_to_arr(str):
    arr = str.split("/")
    for i in range(len(arr)):
        arr[i] = arr[i].split(",")
        arr[i] = list(map(int, arr[i]))

    return arr


def to_C(origin_key, arr):
    temp = key[origin_key]
    for i in range(len(arr)):
        for j in range(len(arr[i])):
            arr[i][j] -= temp
    return arr


def melody_metrix(arr):
    metrix = numpy.zeros((len(arr), 12))
    for i in range(len(arr)):
        count = 0
        for j in range(len(arr[i])):
            metrix[i][arr[i][j]] = metrix[i][arr[i][j]] + 1
            count = count + 1
        for j in range(12):
            metrix[i][j] = metrix[i][j] / count

    return metrix


def input(s, a):
    # str="1,2,3 4,5,6 7,8,9"
    arr = str_to_arr(s)

    # a='#C'
    to_C(a, arr)

    metrix = (melody_metrix(arr))
    print(metrix)
    return metrix

