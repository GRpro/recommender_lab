import csv
import os
import requests
import json
import sys

# upload dataset from https://www.kaggle.com/retailrocket/ecommerce-dataset#events.csv
def main(path):


    with open(os.path.abspath(path)) as csvfile:
        rdr = csv.reader(csvfile, delimiter=',')

        header = next(rdr)
        print("header, ", header)

        url = 'http://localhost:5555/api/events/createMany'

        n = 0
        max = 1000

        uploaded = 0
        events = []

        for row in rdr:
            user = row[1]
            indicator = row[2]
            item = row[3]
            events.append(
                {
                    "subjectId": user,
                    "objectId": item,
                    "indicator": indicator
                }
            )
            n += 1

            if n == max:
                requests.post(url, json=events)
                uploaded += n
                print("batch of ", n, " already uploaded ", uploaded)
                events = []
                n = 0

        if n > 0:

            requests.post(url, json=events)
            uploaded += n
            print("batch of ", n, " already uploaded ", uploaded)


if __name__ == '__main__':
    main(sys.argv[1])