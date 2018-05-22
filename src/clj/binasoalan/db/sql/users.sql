-- :name insert-user
-- :command :execute
-- :result :affected
-- :doc Insert a user returning affected row count
insert into users (username, email, password)
values (:username, :email, :password)

-- :name find-user-by-username
-- :command :query
-- :result :one
-- :doc Find user by username
select * from users
where username = :username

-- :name find-user-by-email
-- :command :query
-- :result :one
-- :doc Find user by email
select * from users
where email = :email

-- :name verify-email
-- :command :execute
-- :result :affected
-- :doc Update verified status of user to true given the token
update users set verified = true
where email = (select email from email_verifications where token = :token)
