#!/usr/bin/python

import random, sys

f1 = open(sys.argv[1], 'w')
f2 = open(sys.argv[2], 'w')

files = [f1]*7 + [f2]*3

for line in sys.stdin.readlines():
  random.choice(files).write(line)
