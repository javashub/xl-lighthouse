#!/bin/bash

source ~/.bashrc;
DAYS=2;
if [ "$#" -eq 1 ] && [[ "$1" =~ ^[0-9]+$ ]] && [ "$1" -lt 30 ]; then
	DAYS=${1}
fi
cur_hostname=$(hostname)
current_date=$(date +'%Y-%m-%d')
TARGET_HOME=${LDP_HOME}/temp/logpack/${current_date}/${cur_hostname}
rm -rf ${LDP_HOME}/temp/logpack/
mkdir -p ${TARGET_HOME}

if [ ! -r "/var/log" ]; then
	echo "The current user does not have access rights to the directory[/var/log], ignore the log files!"
else
	rm -rf ${TARGET_HOME}/system
	mkdir -p ${TARGET_HOME}/system
	find /var/log -maxdepth 1 -type f \( -name "*message*" -o -name "*syslog*" \) -mtime -${DAYS} -exec cp {} ${TARGET_HOME}/system \;
fi


if [ -d "$LDP_HOME/dependency/hadoop/logs" ]; then
	rm -rf ${TARGET_HOME}/hadoop
	mkdir -p ${TARGET_HOME}/hadoop
	find $LDP_HOME/dependency/hadoop/logs  -maxdepth 1 -type f  -mtime -${DAYS} -exec cp {} ${TARGET_HOME}/hadoop \;
fi

if [ -d "$LDP_HOME/dependency/spark/logs" ]; then
        rm -rf ${TARGET_HOME}/spark
        mkdir -p ${TARGET_HOME}/spark
        find $LDP_HOME/dependency/spark/logs  -maxdepth 1 -type f  -mtime -${DAYS} -exec cp {} ${TARGET_HOME}/spark \;
fi

if [ -d "$LDP_HOME/dependency/hbase/logs" ]; then
        rm -rf ${TARGET_HOME}/hbase
        mkdir -p ${TARGET_HOME}/hbase
        find $LDP_HOME/dependency/hbase/logs  -maxdepth 1 -type f  -mtime -${DAYS} -exec cp {} ${TARGET_HOME}/hbase \;
fi

if [ -d "$LDP_HOME/dependency/mysql/log" ]; then
        rm -rf ${TARGET_HOME}/mysql
        mkdir -p ${TARGET_HOME}/mysql
        find $LDP_HOME/dependency/mysql/log  -maxdepth 1 -type f  -mtime -${DAYS} -exec cp {} ${TARGET_HOME}/mysql \;
fi

if [ -d "$LDP_HOME/dependency/zookeeper/logs" ]; then
        rm -rf ${TARGET_HOME}/zookeeper
        mkdir -p ${TARGET_HOME}/zookeeper
        find $LDP_HOME/dependency/zookeeper/logs  -maxdepth 1 -type f  -mtime -${DAYS} -exec cp {} ${TARGET_HOME}/zookeeper \;
fi

if [ -d "$LDP_HOME/dependency/redis" ]; then
        rm -rf ${TARGET_HOME}/redis
        mkdir -p ${TARGET_HOME}/redis
        find $LDP_HOME/dependency/redis/  -maxdepth 1 -type f -name "*redis.log*"  -mtime -${DAYS} -exec cp {} ${TARGET_HOME}/redis \;
fi

if [ -d "$LDP_HOME/dependency/kafka/logs" ]; then
        rm -rf ${TARGET_HOME}/kafka
        mkdir -p ${TARGET_HOME}/kafka
        find $LDP_HOME/dependency/kafka/logs/  -maxdepth 1 -type f -name "*.log*"  -mtime -${DAYS} -exec cp {} ${TARGET_HOME}/kafka \;
fi

if [ -d "$LDP_HOME/dependency/nginx/logs" ]; then
        rm -rf ${TARGET_HOME}/nginx
        mkdir -p ${TARGET_HOME}/nginx
        find $LDP_HOME/dependency/nginx/logs -maxdepth 1 -type f -name "*.log*"  -mtime -${DAYS} -exec cp {} ${TARGET_HOME}/nginx \;
fi

if [ -d "$LDP_HOME/logs/lighthouse-ice" ]; then
        rm -rf ${TARGET_HOME}/lighthouse-ice
        mkdir -p ${TARGET_HOME}/lighthouse-ice
        find $LDP_HOME/logs/lighthouse-ice -maxdepth 1 -type f -name "*.log*"  -mtime -${DAYS} -exec cp {} ${TARGET_HOME}/lighthouse-ice \;
	if [ -d "$LDP_DATA_DIR/ice/nodeoutput" ]; then
        	find $LDP_DATA_DIR/ice/nodeoutput -maxdepth 1 -type f  -mtime -${DAYS} -exec cp {} ${TARGET_HOME}/lighthouse-ice \;
	fi
fi

if [ -d "$LDP_HOME/logs/lighthouse-tasks" ]; then
        rm -rf ${TARGET_HOME}/lighthouse-tasks
        mkdir -p ${TARGET_HOME}/lighthouse-tasks
        find $LDP_HOME/logs/lighthouse-tasks -maxdepth 1 -type f -name "*.log*"  -mtime -${DAYS} -exec cp {} ${TARGET_HOME}/lighthouse-tasks \;
fi

if [ -d "$LDP_HOME/logs/lighthouse-insights" ]; then
        rm -rf ${TARGET_HOME}/lighthouse-insights
        mkdir -p ${TARGET_HOME}/lighthouse-insights
        find $LDP_HOME/logs/lighthouse-insights -maxdepth 1 -type f -name "*.log*"  -mtime -${DAYS} -exec cp {} ${TARGET_HOME}/lighthouse-insights \;
fi

cd ${LDP_HOME}/temp/logpack/${current_date};
tar -zcvf ${cur_hostname}.tar.gz ${cur_hostname} --remove-files
