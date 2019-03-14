import csv
import os
import requests
import sys
import time


def upload_events(events_file):
    with open(os.path.abspath(events_file)) as csvfile:
        rdr = csv.reader(csvfile, delimiter=',')

        header = next(rdr)
        print("header, ", header)

        url = 'http://localhost:5555/api/events/createMany'

        n = 0
        max = 2000

        uploaded = 0
        events = []

        for col in rdr:
            user = col[1]
            indicator = col[2]
            item = col[3]
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


def upload_properties(item_properties_file, available_properties):


    with open(os.path.abspath(item_properties_file)) as csvfile:
        rdr = csv.reader(csvfile, delimiter=',')

        header = next(rdr)
        print("header, ", header)

        url = 'http://localhost:5555/api/objects/updateMultiById'

        n = 0
        max = 2000
        line = 1

        uploaded = 0
        update_requests = []

        for col in rdr:
            object_id = col[1]
            property_key = str(col[2])

            if property_key in available_properties:
                property_val = str(col[3])
                json_body = {
                    "objectId": object_id,
                    "objectProperties": {
                        property_key: property_val
                    },
                    "replace": False
                }
                update_requests.append(
                    json_body
                )

                n += 1

                if n == max:
                    requests.post(url, json=update_requests)
                    uploaded += n
                    print("batch of ", n, " already uploaded ", uploaded, " line ", line)
                    update_requests = []
                    n = 0
            line += 1

        if n > 0:
            print(requests.post(url, json=update_requests))
            uploaded += n
            print("last batch of ", n, " already uploaded ", uploaded)



# upload dataset from https://www.kaggle.com/retailrocket/ecommerce-dataset#events.csv
def main(path):
    events_file = os.path.join(path, 'events.csv')
    item_properties_part1_file = os.path.join(path, 'item_properties_part1.csv')
    item_properties_part2_file = os.path.join(path, 'item_properties_part2.csv')


    # upload only first 100 properties + categoryid
    available_properties = {str(a) for a in range(1, 100)}
    available_properties.add('categoryid')

    start = time.time()

    print("upload events")
    upload_events(events_file)
    events_upload_elapsed_time = time.time() - start

    start_upload_p1 = time.time()
    print("upload properties1")
    upload_properties(item_properties_part1_file, available_properties)
    properties1_upload_elapsed_time = time.time() - start_upload_p1

    start_upload_p2 = time.time()
    print("upload properties2")
    upload_properties(item_properties_part2_file, available_properties)
    properties2_upload_elapsed_time = time.time() - start_upload_p2

    end = time.time()
    print("elapsed time {}s: events {}s, props1 {}s, props2 {}s".format(end - start,
                                                                        events_upload_elapsed_time,
                                                                        properties1_upload_elapsed_time,
                                                                        properties2_upload_elapsed_time))


if __name__ == '__main__':
    main(sys.argv[1])
