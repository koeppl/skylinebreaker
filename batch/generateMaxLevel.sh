#!/bin/zsh
file=algorithm.properties
maxdim=10
for levels in `seq 3000 1 4500`; do
	j=$levels
	level=()
	i=1
	while [[ $j -ne 0 ]]; do
#		local m
		((m = (j % maxdim + 1) * 10))
		level[$i]=$m
		((j /= maxdim))
		((++i))
	done
	levellength=${#level}
	maxlevel=$level[1]
	valid=1
	for j in `seq 2 $levellength`; do
		[[ $maxlevel -eq $level[$j] ]] && continue
		if [[ $maxlevel -lt $level[$j] ]]; then
			valid=0
			break
		else
			maxlevel=$level[$j]
		fi
	done
	[[ valid -eq 1 ]] && echo $level | tr ' ' ','
done


exit 1



echo -n 'MAX_LEVELS = ' 
for dimensions in `seq 3 5`; do
for levels in `seq 10 10 100`; do
	echo -n '{ '
	for dimension in `seq 1 $dimensions`; do
		echo -n "$levels" 
		[[ $dimension -lt $dimensions ]] && echo -n ','
	done 
	echo -n '}; '
done
done

exit 1

distributions=(equal corr anti gauss normal)






for dimensions in `seq 3 5`; do
for levels in `seq 10 10 100`; do
for distribution in $distributions; do
	echo -n 'MAX_LEVELS = {' >> $file
	for dimension in `seq 1 $dimensions`; do
		echo -n "$levels" 
		[[ $dimension -lt $dimensions ]] && echo -n ','
	done >> $file
	echo '}' >> $file
	echo 'DISTRIBUTION = ' $distribution >> $file
#	cat rest.property >> $file
#	CLASSPATH="../../PreferenceSQL/bin:../../PreferenceXXL/bin:../../PreferenceBase/bin:.:../../FlatLCDataGenerator/bin" java test.performance.algorithms.habil.RunAlgorithmTest
done
done
done
#cat > algorithm.properties <<EOF
#done


