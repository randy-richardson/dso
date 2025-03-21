#!/bin/sh
#
# Copyright Terracotta, Inc.
# Copyright IBM Corp. 2024, 2025
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#




workdir=`dirname $0`
workdir=`cd ${workdir} && pwd`
TC_HOME=${workdir}/../../server

# Set the path to your Terracotta server home here

if [ ! -f $TC_HOME/bin/start-tc-server.sh ]; then
  echo "Modify the script to set TC_HOME" 
  exit -1
fi

exec $TC_HOME/bin/start-tc-server.sh&
