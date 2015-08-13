#/bin/python
import sys

#fr = open('set.txt')
#fw = open('setm.txt')
for line in sys.stdin:
	rstList = []
	items=line.split("\t");
	#print items
	if items[0].strip() == "P10":
		print items[2].strip()
	if items[0].strip() == "P20":
		print items[2].strip()
	if items[0].strip() == "P30":
		print items[2].strip()
	if items[0].strip() == "map":
		print items[2].strip()
