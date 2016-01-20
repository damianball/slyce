# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table qasession (
  id                        bigint not null,
  host_id                   bigint,
  datetime_start            timestamp,
  datetime_end              timestamp,
  constraint pk_qasession primary key (id))
;

create table question (
  id                        bigint not null,
  qa_session_id             bigint,
  asked_by_id               bigint,
  text                      varchar(255),
  datetime_asked            timestamp,
  answered_by_id            bigint,
  answer_text               varchar(255),
  answer_image_url          varchar(255),
  datetime_answered         timestamp,
  constraint pk_question primary key (id))
;

create table user (
  id                        bigint not null,
  name                      varchar(255),
  constraint uq_user_name unique (name),
  constraint pk_user primary key (id))
;

create sequence qasession_seq;

create sequence question_seq;

create sequence user_seq;

alter table qasession add constraint fk_qasession_host_1 foreign key (host_id) references user (id) on delete restrict on update restrict;
create index ix_qasession_host_1 on qasession (host_id);
alter table question add constraint fk_question_qaSession_2 foreign key (qa_session_id) references qasession (id) on delete restrict on update restrict;
create index ix_question_qaSession_2 on question (qa_session_id);
alter table question add constraint fk_question_askedBy_3 foreign key (asked_by_id) references user (id) on delete restrict on update restrict;
create index ix_question_askedBy_3 on question (asked_by_id);
alter table question add constraint fk_question_answeredBy_4 foreign key (answered_by_id) references user (id) on delete restrict on update restrict;
create index ix_question_answeredBy_4 on question (answered_by_id);



# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists qasession;

drop table if exists question;

drop table if exists user;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists qasession_seq;

drop sequence if exists question_seq;

drop sequence if exists user_seq;

