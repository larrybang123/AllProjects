ssh -n $1: nohup java -jar ~/ece419/ECE419_DistributedSystem/ms2-server.jar $2 $3 $4 ERROR &
# 1 is IP, 2 is port, 3 is cache size, 4 is cache strategy