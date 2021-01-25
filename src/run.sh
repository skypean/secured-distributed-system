cp filesample.txt data1G.txt
# gen file 1G
for ((n=0;n<20;n++))
do
	cat data1G.txt data1G.txt > temp
	mv temp data1G.txt	
done

# run file 1G
for ((n=0;n<$1;n++))
do
	java OriginalNode/OriginalNode data1G.txt 3		
done

# gen file 2G

cat data1G.txt data1G.txt > temp
mv temp data2G.txt
rm data1G.txt	

# run file 2G
for ((n=0;n<$1;n++))
do
	java OriginalNode/OriginalNode data2G.txt 3		
done

# gen file 4G
cat data2G.txt data2G.txt > temp
mv temp data4G.txt
rm data2G.txt	

# run file 4G
for ((n=0;n<$1;n++))
do
	java OriginalNode/OriginalNode data4G.txt 3		
done

# gen file 8G
cat data4G.txt data4G.txt > temp
mv temp data8G.txt
rm data4G.txt

# run file 8G
for ((n=0;n<$1;n++))
do
	java OriginalNode/OriginalNode data8G.txt 3		
done
