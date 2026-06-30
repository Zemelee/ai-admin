# 实习管理 AI 智能体系统（MVP）


---

```bash
systemctl start docker
docker start minio

docker compose up -d
mvn clean package -DskipTests
```


## 角色与默认账号（种子数据）

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 监管者 supervisor | `admin` | `123456` |
| 实习指导教师 teacher | `teacher01` | `123456` |
| 实习学生 student | `student01` | `123456` |
| 企业指导人员 mentor | `mentor01` | `123456` |

