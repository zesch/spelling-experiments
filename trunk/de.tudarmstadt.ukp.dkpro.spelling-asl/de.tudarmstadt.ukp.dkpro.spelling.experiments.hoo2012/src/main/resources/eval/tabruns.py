# File:     tabruns.py
#
# Author:	George Narroway 
# Modified:	23 April 2012
# Edited by:  Ilya Anisimoff
# Version:      0.6
#
# Copyright (c) Centre for Language Technology, Macquarie University
# Not to be used for commercial purposes.
#
# Purpose:
# Tabulates a directory of results files.
# 
# usage: tabruns.py [-h] [-f {csv,latex}] [-i] [-d] [-m {prf,p,r,pr}]
#                   [-o {alpha,precision,recall,fscore}]
#                   [-s {detection,recognition,correction}] [-n {real,pc}]
#                   [-p PRECISION] [-b]
#                   resultsdir
# 
# 
# positional arguments:
#   resultsdir
# 
# optional arguments:
#   -h, --help            show this help message and exit
#   -f {csv,latex}, --format {csv,latex}
#   -i, --showteamids
#   -d, --descending
#   -m {prf,p,r,pr}, --measures {prf,p,r,pr}
#   -o {alpha,precision,recall,fscore}, --order {alpha,precision,recall,fscore}
#   -s {detection,recognition,correction}, --score {detection,recognition,correction}
#   -n {real,pc}, --number {real,pc}
#   -p PRECISION, --precision PRECISION
#   -b, --best

from argparse import ArgumentParser, ArgumentTypeError

import os
import xml.etree.cElementTree as ET

MEASURES = ['precision', 'recall', 'fscore']


def process(resultsdir, output_format, showteamids, measures, sort_order,
            descending, scoretype, num_format, precision, best):

    """Processes a directory of results files and tabulates it."""

    files = compile_files(resultsdir)

    data = [parse(f, measures, scoretype) for f in files]
    flat_data, best_prf = flatten(data, num_format, precision)


    sorted_data = sort(flat_data, sort_order, descending)

    if output_format == "latex":
        print_latex(sorted_data, scoretype, measures, showteamids, best_prf, best)
    else:
        print_csv(sorted_data, scoretype, measures, showteamids, best_prf, best)
    
    

def compile_files(root):
    """Generates a list of full paths corresponding 
    to the files in a given source directory.
    """
    files = [os.path.join(root, f) for f in os.listdir(root)
             if not f.startswith(".")]
    
    return [f for f in files if os.path.isfile(f)]



def parse(f, measures='prf', scoretype='detection'):
    """Parses the system info and scores data out of a results file.
    Only include measures and scoretypes specified in the arguments.
    Format:
    d['sysid']
    d['runid']
    d['scores']['precision']
    """
    root = ET.parse(f)
    output = dict()

    # Get system and run info
    results = root.getroot()
    output['sysid'] = results.get('sysid')
    output['runid'] = results.get('runid')   
    
    # Expand measures args into element names
    measurenames = list()
    
    if "p" in measures:
        measurenames.append("precision")
    if "r" in measures:
        measurenames.append("recall")
    if "f" in measures:
        measurenames.append("fscore")
        
    # Iterate scores and measures, saving relevant ones
    outputscores = dict()
    score = root.find("scores/%s" % scoretype)

    output['scores'] = dict((e.tag, e.text) for e in score.iter()
                             if e.tag in measurenames)

    return output



def flatten(data, num_format, precision):
    """Partially flatten a list of dicts into a list of lists.
    Order: [sysid, runid, (precision, 0.1), (recall, 0.2), (fscore, 1)]]
    """
    output = list()
    best_prf = [-1.0, -1.0, -1.0]
    
    for d in data:
        # Create the system and run data
        row = [('sysid', d.get('sysid', ''))]
        row.append(('runid', d.get('runid', '')))
        
        # Flatten the scores and add to the row
        score = [(m, float(d['scores'][m])) for m in MEASURES if m in d['scores']]
        
        if num_format == 'pc':
            score = [(m, x * 100) for m, x in score]
        
        if precision > 0:
            score = [(m, round(x, precision)) for m, x in score]
            
        new_prf = [x for m, x in score]
        for i in range(len(best_prf)):
            best_prf[i] = max(best_prf[i], new_prf[i])
        
        score = [(m, str(x)) for m, x in score]
                
        row.extend(score)

        output.append(row)
    best_prf = [str(x) for x in best_prf]
    return output, best_prf
    


def sort(scores, order="alpha", descending=False):
    """Sorts a list of lists of tuples.

    scores -- a list where each item is [(k1,v1), (k2,v2)...]
    So we find the column where the k matches the sort order,
    then sort scores based on the corresponding v.
    """
    row = scores[0]

    if order == "alpha":
        order == "sysid"
        
    sort_column = get_sort_column(order, row)

    # Sort by the value of the correct column
    scores.sort(key=lambda x: x[sort_column][1])

    if descending:
        scores.reverse()
    
    return scores



def get_sort_column(name, row):
    """Goes through a list of pairs and finds the index of
    the item where the first element of pair matches 'name'.
    """
    for i,(k,v) in enumerate(row):
        if k == name:
            return i

    return 0
            


def print_latex(scores, scoretype, measures, showteamids, best_prf, best):
    """Prints a list of scores to standard out, in latex format."""
    output = list()
    output.append(r"\begin{table}")

    # Center format for 'runid' and every measure
    score_columns = "c" * (1 + len(measures))
    col_format = "|%s|" % score_columns

    if showteamids:
        col_format = "|l%s" % col_format

    output.append(r"\begin{tabular}{%s}\hline" % col_format)

    # Header
    output.append(get_header(measures, showteamids, " & ", r" \\\hline"))

    # Scores
    output.extend(format_scores(scores, showteamids, best, best_prf, ('{\\bf ', '}')," & ", r" \\"))

    # Other stuff
    output.append(r"\hline")
    output.append(r"\end{tabular}")
    caption = r"\Caption{Scores for %s}" % scoretype   
    output.append(caption)
    output.append(r"\end{table}")

    print "\n".join(output)



def print_csv(scores, scoretype, measures, showteamids, best_prf, best):
    """Prints a list of scores to standard out, in CSV format."""
    output = list()

    # Header
    output.append(get_header(measures, showteamids))

    # Scores
    output.extend(format_scores(scores, showteamids, best, best_prf))

    print "\n".join(output)

    

def get_header(measures, showteamids, separator=",", tail=""):
    if showteamids:
        headers = ["Team", "Run"]
    else:
        headers = ["Run"]

    if "p" in measures:
        headers.append("Precision")
    if "r" in measures:
        headers.append("Recall")
    if "f" in measures:
        headers.append("F-Score")

    header = separator.join(headers)

    if tail:
        header = "%s%s" % (header, tail)

    return header



def format_scores(scores, showteamids, best, best_prf, best_brackets=('[',']'), separator=",", tail=""):
    """Formats rows of scores into a format using given separator
    and tail arguments.
    separator -- Things like "," for csv, " & " for latex.
    tail -- Optional arg like "\\" for latex table rows.
    """
    output = list()
    start = 1
    if showteamids:
        start = 0
    best_open, best_close = best_brackets
    for score in scores:
        if best:
            for i in range(len(best_prf)):
                prf = score[i + 2][1]
                if prf == best_prf[i]:
                    score[i + 2] = (score[i + 2][0], best_open + prf + best_close)
        row = separator.join([s[1] for s in score[start:]])
        output.append(row)

    if tail:
        output = ["%s%s" % (row, tail) for row in output]

    return output

    

def directory(s):
    if not os.path.isdir(s):
        msg = "%r is not a directory" % s
        raise ArgumentTypeError(msg)
    
    return s



if __name__ == '__main__':    
    # Set up the command line utility
    parser = ArgumentParser(description="Tabulates a directory of results files.")
    parser.add_argument("-f", "--format", default="csv", dest="format", \
                        choices=['csv', 'latex'])
    parser.add_argument("-i", "--showteamids", action="store_true", dest="showteamids",
                        default=False)
    parser.add_argument("-d", "--descending", action="store_true", dest="descending",
                        default=False)
    parser.add_argument("-m", "--measures", default="prf", dest="measures", \
                        choices=['prf','p','r','pr'])
    parser.add_argument("-o", "--order", default="alpha", dest="sort_order", \
                        choices=['alpha', 'precision', 'recall', 'fscore'])
    parser.add_argument("-s", "--score", default='detection', dest="scoretype", \
                        choices=['detection', 'recognition', 'correction'])
    parser.add_argument("-n", "--number", default='real', dest="num_format", \
                        choices=['real', 'pc'])
    parser.add_argument("-p", "--precision", default=-1, dest="precision", type=int)
    parser.add_argument("-b", "--best", action="store_true", dest="best",
                        default=False)
    parser.add_argument("resultsdir", type=directory)

    # Get the arguments and execute
    args = parser.parse_args()
    
    process(args.resultsdir, args.format, args.showteamids, args.measures,
            args.sort_order, args.descending, args.scoretype, args.num_format, 
            args.precision, args.best)


        



