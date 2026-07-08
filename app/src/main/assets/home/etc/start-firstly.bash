# Launch after data installation is complete.

if [ -d "$AON" ] || [ "$AOK" ]; then
chmod 777 $AON/*/*
for vadd in $AON/* $AOK/*; do
    if [ -f "$vadd/firstly_start.bash" ]; then
    echo "Firstly shell: $vadd/firstly_start.bash"
    $vadd/firstly_start.bash &
    elif [ -f "$vadd/firstly_start.sh" ]; then
    echo "Firstly shell: $vadd/firstly_start.sh"
    $vadd/firstly_start.sh &
    fi
done
wait
fi
