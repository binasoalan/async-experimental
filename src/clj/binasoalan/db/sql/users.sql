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
