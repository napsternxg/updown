#!/usr/bin/python
import re, sys

DEBUG=False
NaN = float('nan')
statRE=re.compile(r"^STAT: (\S+) (\S+) (.*)")
(fold, prep, exp, n, acc, fpos, fneg) = (None,)*7
skip=True
folds = set()
tables = {}

def debug(s):
  if DEBUG:
    print('>'+str(s))

for line in sys.stdin.readlines():
  line = line.rstrip()
  m=statRE.match(line)
  if (m):
    (fold, prep, exp) = m.groups()
    folds.add(fold)
    if not prep in tables:
      tables[prep] = {}
    if not exp in tables[prep]:
      tables[prep][exp] = {}
    for i in "n acc fpos fneg".split(" "):
      if not i in tables[prep][exp]:
        tables[prep][exp][i] = {}
      if not fold in tables[prep][exp][i]:
        tables[prep][exp][i][fold] = NaN
    skip=False
    continue
  if re.match(r"^Per",line):
    skip=True
    continue
  if (skip): continue
  m= re.match(r"^\s+N\s+(\d+)",line)
  if (m):
    (n,)=m.groups()
    tables[prep][exp]['n'][fold] = float(n)
    debug(tables[prep][exp]['n'])
    continue
  m= re.match(r"^\s+Accuracy\s+(\d+.\d+)",line)
  if (m):
    (acc,)=m.groups()
    tables[prep][exp]['acc'][fold] = float(acc)
    debug(tables[prep][exp]['acc'])
    continue
  m= re.match(r"^\s+positive .* (\d+\.\d+)$",line)
  if (m):
    (fpos,)=m.groups()
    tables[prep][exp]['fpos'][fold] = float(fpos)
    debug(tables[prep][exp]['fpos'])
    continue
  m= re.match(r"^\s+negative .* (\d+\.\d+)$",line)
  if (m):
    (fneg,)=m.groups()
    tables[prep][exp]['fneg'][fold] = float(fneg)
    debug(tables[prep][exp]['fneg'])
    continue
  #m= re.match(r"^Exception",line)
  #if (m):
    #tables[prep][exp]['n'] = NaN
    #tables[prep][exp]['acc'] = NaN
    #tables[prep][exp]['fpos'] = NaN
    #tables[prep][exp]['fneg'] = NaN
    #debug("exception")
    #continue

prep_set = set(tables.keys())
exp_set = set()
val_set = set()
fold_set = folds
for prep in tables:
  for exp in tables[prep]:
    exp_set.add(exp)
    for val in tables[prep][exp]:
      val_set.add(val)
prep_set = sorted(list(prep_set))
exp_set = sorted(list(exp_set))
val_set = sorted(list(val_set))
fold_set = sorted(list(fold_set))

sums={}
def print_csv_itable(fold,val):
  lines = ['']*(1+len(prep_set))
  lines[0] += '"(%s)",'%(val)
  for i,prep in enumerate(prep_set):
    lines[1+i] += '"%s",'%str(prep)

  for exp in exp_set:
    lines[0] += '"%s",'%str(exp)
    for i,prep in enumerate(prep_set):
      if not prep in sums: sums[prep] = {}
      if not exp in sums[prep]: sums[prep][exp] = {}
      if not val in sums[prep][exp]: sums[prep][exp][val] = 0.0
      if prep in tables and exp in tables[prep] and val in tables[prep][exp] and fold in tables[prep][exp][val]:
        sums[prep][exp][val] += tables[prep][exp][val][fold]
        if val == 'n':
          try: 
            lines[1+i] += '%d,'%int(tables[prep][exp][val][fold])
          except:
            lines[1+i] += '%.2f,'%(tables[prep][exp][val][fold])
        else:
          lines[1+i] += '%.2f,'%(tables[prep][exp][val][fold])
      else: lines[1+i] += ','
  return lines

def print_csv_itable_sums(val):
  lines = ['']*(1+len(prep_set))
  lines[0] += '"(%s)",'%(val)
  for i,prep in enumerate(prep_set):
    lines[1+i] += '"%s",'%str(prep)

  for exp in exp_set:
    lines[0] += '"%s",'%str(exp)
    for i,prep in enumerate(prep_set):
      lines[1+i] += '%.2f,'%(sums[prep][exp][val] / len(fold_set))
  return lines

def print_csv():
  sections = []
  for fold in fold_set:
    lines = [ l + ',' for l in print_csv_itable(fold,'n')]
    lines = [ l + r + ',' for (l,r) in zip(lines, print_csv_itable(fold,'acc'))]
    lines = [ l + r + ',' for (l,r) in zip(lines, print_csv_itable(fold,'fpos'))]
    lines = [ l + r for (l,r) in zip(lines, print_csv_itable(fold,'fneg'))]
    sections.append((fold,lines))
  lines2 = [ l + ',' for l in print_csv_itable_sums('n')]
  lines2 = [ l + r + ',' for (l,r) in zip(lines2, print_csv_itable_sums('acc'))]
  lines2 = [ l + r + ',' for (l,r) in zip(lines2, print_csv_itable_sums('fpos'))]
  lines2 = [ l + r for (l,r) in zip(lines2, print_csv_itable_sums('fneg'))]
  sections.append(("AVERAGE",lines2))
  return sections

sections = print_csv()
for name,lines in sections:
  print("\n"+name)
  for line in lines:
    print(line)

sys.exit()



import pprint
pprint.pprint(tables)

