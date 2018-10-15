import csv
import os
import requests
import json

def main():

    events = []
    with open(os.path.abspath(os.path.join(os.path.realpath(__file__), os.pardir, 'indicators1.csv'))) as csvfile:
        spamreader = csv.reader(csvfile, delimiter=',')
        for row in spamreader:
            print(row)
            user = row[0]
            indicator = row[1]
            item = row[2]
            events.append(
                {
                    "subjectId": user,
                    "objectId": item,
                    "indicator": indicator
                }
            )
    url = 'http://localhost:5555/api/events/batch'
    requests.post(url, json=events)


if __name__ == '__main__':
    main()