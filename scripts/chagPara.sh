#########################################################################
# File Name: scripts/chagPara.sh
# Author: ma6174
# mail: ma6174@163.com
# Created Time: Thu Nov  6 21:54:56 2014
#########################################################################
#!/bin/bash

name="fbOrigWeight="
old=$name"0.5"
#array=(10 20 30)
#array=(0.0 0.2	0.4	0.6	0.8	1.0)
array=(0.0)

for val in ${array[@]}  
do
	echo "######val:######"${val}
	new=$name${val}
	echo $new
	sed  's/'$old'/'$new'/g' parameterFile.txt.bak > 

	java -cp "./src:./lucene-4.3.0/*" QryEval parameterFile.txt
	perl scripts/trec.perl > test.txt
	java autoTest
done

