create table exodus_status (
    tabell              text        not null primary key,
    iterator            text,
    job_status          text        not null default 'OK'
        check (job_status in ('OK', 'PAGINERER', 'NY_BASELINE')),
    antall_rader_hentet bigint      not null default 0,
    sist_oppdatert      timestamp   not null default current_timestamp
);
