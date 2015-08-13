#!/bin/pyhton
'''
00 url
0.00 keywords
0.00 title
0.00 body
0.00 inlink
'''
fields=['url','keywords','title','body','inlink']
wgt=[0.03 ,0.02 ,0.15 ,0.70, 1.0];
#wgt=[0.1 ,0.2 ,0.3 ,0.4, 0.2];
#fp =open("/Users/lipingxiong/Documents/workspace/hw1/scripts/wgt.txt")
#for l in fp:
#	wgt=l.split()

fr = open('/Users/lipingxiong/Documents/workspace/hw1/query/set.txt')
#fw = open('setm.txt')
for line in fr:
	#qStr= "#AND("
	lineN=line.split(":")[0].strip()
	terms=line.split(":")[1].split();
	qStr= lineN+":"
	#10:cheap internet
	for t in terms:
		wsumStr= " #WSUM(" 
		for i in range( len(fields) ):
			wsumStr += " "+ str(wgt[i])+" " + t +"."+ fields[i] + " "
		wsumStr +=")"
		#print wsumStr
		qStr+=wsumStr
	qStr += ""
	#qStr += ")"
	print qStr






	#AND (
	    #WSUM(0.1 time.url      0.2 time.title      0.3 time.inlink      0.4 time.body)
		    #WSUM(0.1 traveler.url  0.2 traveler.title  0.3 traveler.inlink  0.4 traveler.body)
			    #WSUM(0.1 wife.url      0.2 wife.title      0.3 wife.inlink      0.4 wife.body))

