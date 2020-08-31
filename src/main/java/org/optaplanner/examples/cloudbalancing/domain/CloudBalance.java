/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.examples.cloudbalancing.domain;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("CloudBalance")
public class CloudBalance extends AbstractPersistable {

    private List<CloudComputer> computerList;

    private List<CloudProcess> processList;

    public CloudBalance() {
    }

    public CloudBalance(long id, List<CloudComputer> computerList, List<CloudProcess> processList) {
        super(id);
        this.computerList = computerList;
        this.processList = processList;
    }

    public List<CloudComputer> getComputerList() {
        return computerList;
    }

    public void setComputerList(List<CloudComputer> computerList) {
        this.computerList = computerList;
    }

    public List<CloudProcess> getProcessList() {
        return processList;
    }

    public void setProcessList(List<CloudProcess> processList) {
        this.processList = processList;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************
}
