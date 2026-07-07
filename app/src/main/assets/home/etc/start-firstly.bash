# Launch after data installation is complete.

if [ -d "$AON" ] || [ "$AOK" ];then
chmod 777 $AON/*/*.sh $AOK/*/*.sh
for vadd in $AON/*/firstly_start.sh $AOK/*/firstly_start.sh; do
    if [ -f "$vadd" ];then
    echo "firstly shell: $vadd"
    $vadd &
    fi
done
wait
fi
