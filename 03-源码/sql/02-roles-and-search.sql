INSERT INTO t_role(id,code,name) VALUES(1,'USER','普通用户'),(2,'REVIEWER','内容审核员'),(3,'ADMIN','系统管理员');
ALTER TABLE t_note ADD FULLTEXT INDEX ft_note_text (title,body) WITH PARSER ngram;
