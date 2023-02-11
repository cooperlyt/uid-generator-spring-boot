/*
 * Copyright (c) 2017 Baidu, Inc. All Rights Reserve.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.coopersoft.cloud.uid;

import cc.coopersoft.cloud.uid.utils.DockerUtils;
import cc.coopersoft.cloud.uid.utils.NetUtils;
import cc.coopersoft.cloud.uid.worker.WorkerIdAssigner;
import cc.coopersoft.cloud.uid.worker.dao.WorkerNodeDao;
import cc.coopersoft.cloud.uid.worker.entity.WorkerNodeEntity;
import cc.coopersoft.cloud.uid.worker.entity.WorkerNodeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Represents an implementation of {@link WorkerIdAssigner},
 * the worker id will be discarded after assigned to the UidGenerator
 *
 * @author yutianbao
 */

@Slf4j
public class DisposableWorkerIdAssigner implements WorkerIdAssigner {

    private final WorkerNodeDao workerNodeDAO;

    public DisposableWorkerIdAssigner(WorkerNodeDao workerNodeDAO) {
        this.workerNodeDAO = workerNodeDAO;
    }


    /**
     * Assign worker id base on database.<p>
     * If there is host name & port in the environment, we considered that the node runs in Docker container<br>
     * Otherwise, the node runs on an actual machine.
     *
     * @return assigned worker id
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public long assignWorkerId() {
        // build worker node entity
        WorkerNodeEntity workerNodeEntity = buildWorkerNode();

        return workerNodeDAO
            .getWorkerNodeByHostPort(workerNodeEntity.getHostName(), workerNodeEntity.getPort())
            .map(WorkerNodeEntity::getId)
            .orElseGet(() -> {
                workerNodeDAO.addWorkerNode(workerNodeEntity);
                return workerNodeEntity.getId();
            });
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public long assignFakeWorkerId() {
        return buildFakeWorkerNode().getId();
    }

    /**
     * Build worker node entity by IP and PORT
     */
    private WorkerNodeEntity buildWorkerNode() {
        WorkerNodeEntity workerNodeEntity = new WorkerNodeEntity();
        if (DockerUtils.isDocker()) {
            workerNodeEntity.setType(WorkerNodeType.CONTAINER.value());
            workerNodeEntity.setHostName(DockerUtils.getDockerHost());
            workerNodeEntity.setPort(DockerUtils.getDockerPort());
        } else {
            workerNodeEntity.setType(WorkerNodeType.ACTUAL.value());
            workerNodeEntity.setHostName(NetUtils.getLocalAddress());
            workerNodeEntity.setPort(System.currentTimeMillis() + "-" + RandomUtils.nextInt(100000));
        }

        return workerNodeEntity;
    }

    private WorkerNodeEntity buildFakeWorkerNode() {
        WorkerNodeEntity workerNodeEntity = new WorkerNodeEntity();
        workerNodeEntity.setType(WorkerNodeType.FAKE.value());
        if (DockerUtils.isDocker()) {
            workerNodeEntity.setHostName(DockerUtils.getDockerHost());
            workerNodeEntity.setPort(DockerUtils.getDockerPort() + "-" + RandomUtils.nextInt(100000));
        } else {
            workerNodeEntity.setHostName(NetUtils.getLocalAddress());
            workerNodeEntity.setPort(System.currentTimeMillis() + "-" + RandomUtils.nextInt(100000));
        }
        return workerNodeEntity;
    }
}
