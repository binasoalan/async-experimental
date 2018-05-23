-- :name insert-verification
-- :command :execute
-- :result :affected
-- :doc Insert email verification returning affected row count
insert into email_verifications (token, email)
values (:token, :email)

-- :name delete-verification
-- :command :execute
-- :result :affected
-- :doc Delete email verification returning affected row count
delete from email_verifications
where token = :token

-- :name find-token
-- :command :query
-- :result :one
select token from email_verifications
where token = :token
