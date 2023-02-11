package cc.coopersoft.cloud.uid.worker.jdbc;

import cc.coopersoft.cloud.uid.worker.DisposableWorkerIdAssigner;
import cc.coopersoft.cloud.uid.worker.entity.WorkerNodeEntity;
import cc.coopersoft.cloud.uid.worker.jdbc.mapper.JDBCWorkerNodeMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@ConditionalOnClass({JdbcOperations.class})
@Component
@MapperScan({"cc.coopersoft.cloud.uid.worker.jdbc.mapper"})
public class JDBCWorkerIdAssigner extends DisposableWorkerIdAssigner {

  private final JDBCWorkerNodeMapper jdbcWorkerNodeMapper;

  public JDBCWorkerIdAssigner(JDBCWorkerNodeMapper jdbcWorkerNodeMapper) {
    this.jdbcWorkerNodeMapper = jdbcWorkerNodeMapper;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  protected long assignWorkerId(WorkerNodeEntity workerNodeEntity) {
    return jdbcWorkerNodeMapper
        .getWorkerNodeByHostPort(workerNodeEntity.getHostName(), workerNodeEntity.getPort())
        .map(WorkerNodeEntity::getId)
        .orElseGet(() -> {
          jdbcWorkerNodeMapper.addWorkerNode(workerNodeEntity);
          return workerNodeEntity.getId();
        });
  }
}
