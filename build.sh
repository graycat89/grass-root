#! /bin/bash

# the num of lines stored in each partition
size=$1
inputFile=$2
diff_on=$3

# set up environment variables
echo "export PARTITION_SIZE=$size" > ./.env-local
if [[ diff_on -eq 1 ]]
then
    echo "set diff_on=true"
    echo "export DIFF_ON=true" >> ./.env-local
fi

rm -rf ./tmp/*

lines=$(wc -l < $inputFile)

for ((start=1; start<=lines; start+=size)); do
   end=$((start+size-1))
   output="output-$start-$end.txt"
#   sed -n "$start,$end"'p' $inputFile | nl -b a > "./tmp/$output"
    sed -n "$start,$end"'p' $inputFile > "./tmp/$output"
done

echo "Done processing $inputFile with partition size $size with diff_on=$diff_on"