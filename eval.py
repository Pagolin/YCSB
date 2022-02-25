import csv
import sys
import os

types = ["READ", "UPDATE", "READ-MODIFY-WRITE", "INSERT"]

actual = ("actual_throughput", "[OVERALL], Throughput(ops/sec)")
strings = [
        ("{}_avg", "[{}], AverageLatency(us)"),
        ("{}_min", "[{}], MinLatency(us)"),
        ("{}_max", "[{}], MaxLatency(us)"),
        ("{}_95", "[{}], 95thPercentileLatency(us)"),
        ("{}_99", "[{}], 99thPercentileLatency(us)"),
        ]

def parse_file(fpath):
    parsed_info = {}
    with open(fpath) as f:
        contents = f.readlines()
    for line in contents:
        # short circuit for [OVERALL] result
        if line.startswith(actual[1]):
            parsed_info[actual[0]] = line.split()[-1]
            continue
        # go over the strings and combine them with the types
        for s in strings:
            for typ in types:
                search_string = s[1].format(typ)
                # match for the different lines in the txt file to put them into the dict
                if line.startswith(search_string):
                    parsed_info[s[0].format(typ)] = line.split()[-1]
                    break # labelled breaks aren't a thing in Python but that may help a bit
    return parsed_info


def main(argv):
    all_results = []
    i = 0
    # iterate through directories
    for entry in os.scandir(argv[1]):
        if entry.is_file():
            continue
        framework = entry.name
        for entry_flow in os.scandir(entry):
            if entry_flow.is_file():
                continue
            workflow = entry_flow.name
            for entry_tp in os.scandir(entry_flow):
                if entry_tp.is_file():
                    continue
                throughput = entry_tp.name
                for f in os.scandir(entry_tp):
                    if f.is_file():
                        i += 1
                        parsed = parse_file(f)
                        parsed["throughput"] = throughput
                        parsed["workflow"] = workflow
                        parsed["framework"] = framework
                        parsed["run"] = f.name.split('.')[0]
                        all_results.append(parsed)
    print("Parsed {} files.".format(i))
    
    # prepare order of csv fields
    fieldnames = ["framework", "workflow", "throughput", "run", "actual_throughput"]
    for t in types:
        for (s, _) in strings:
            fieldnames.append(s.format(t))

    # CSV export
    with open(argv[2], 'w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)

        writer.writeheader()
        writer.writerows(all_results)


if __name__ == '__main__':
    main(sys.argv)
