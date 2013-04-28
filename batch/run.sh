#!/bin/zsh
maxcpu='16'
cpumod='3'

CPUs=`seq 1 $maxcpu | tr '\n' ','`

cfg=algorithm.properties
outdir='/home/niki/tmp/ParallelTests'
completedir='/home/niki/tmp/finished'

low_ALGORITHMS="BNL++, HexagonMemOptFLC, SkylineBuster, SkylineBusterP, SkylineShooter, SkylineShooterP, SkylineShooterPS"
high_ALGORITHMS="BNL++, SkylineBuster, SkylineBusterP, SkylineShooter, SkylineShooterP, SkylineShooterPS"
PARALLEL_ALGORITHMS=(SkylineShooter SkylineShooterP SkylineShooterPS)

low_INPUT_SIZE='1000, 2000, 3000, 4000, 5000'
med_INPUT_SIZE='10000, 20000, 30000, 40000, 50000'
high_INPUT_SIZE='100000, 200000, 300000, 400000, 500000'

function invoke {
		mkdir -p "$outdir" &&
		CLASSPATH="../../PreferenceSQL/bin:../../PreferenceXXL/bin:../../PreferenceBase/bin:.:../../FlatLCDataGenerator/bin" java test.performance.algorithms.habil.RunAlgorithmTest
}

rm -r "$outdir"
mkdir -p "$completedir"

for range_size_name in low high; do
	eval algorithms=\$${range_size_name}_ALGORITHMS

	for input_size_name in low med high; do
		eval input_size=\$${input_size_name}_INPUT_SIZE

		for maxlevels in `cat ${range_size_name}_levels`; do

			
			for algo in `echo $PARALLEL_ALGORITHMS`; do
				echo "MAX_LEVELS = { $maxlevels }" > $cfg
				echo "INPUT_SIZE = $input_size " >> $cfg
				echo "RUNTIME = false" >> $cfg
				echo "SCALE = true" >> $cfg
				echo "ALGORITHMS = $algo" >> $cfg
				echo "CPUs=$CPUs" >> $cfg
				cat rest.properties >> $cfg
				invoke
				for file in $outdir/datFile*; do
					mv $file $completedir/${input_size_name}_${maxlevels}_`echo $file | sed 's@.*datFile\(.*\)Scale\(.*\)@\1\2@g'`
				done
				rm -r "$outdir"
			done


			for cpu in `seq 1 $cpumod $maxcpu`; do
				echo "MAX_LEVELS = { $maxlevels }" > $cfg
				echo "INPUT_SIZE = $input_size " >> $cfg
				echo "RUNTIME = true" >> $cfg
				echo "SCALE = false" >> $cfg
				echo "ALGORITHMS = $algorithms" >> $cfg
				echo "CPUs=$cpu" >> $cfg
				cat rest.properties >> $cfg
				invoke
				for file in $outdir/datFile*; do
					mv $file $completedir/${input_size_name}_${maxlevels}_${cpu}_`echo $file | sed 's@.*datFile\(.*\)Runtime\(.*\)@\1\2@g'`
				done
				rm -r "$outdir"
			done



		done
	done
done


