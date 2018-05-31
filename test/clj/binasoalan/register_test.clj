(ns binasoalan.register-test
  (:require [binasoalan.test-utils :refer [route-to with-user path-driver db-spec]]
            [clojure.java.jdbc :as jdbc]
            [clojure.test :refer :all]
            [etaoin.api :refer :all]
            [etaoin.keys :as k]))

(defn- delete-registered-user []
  (jdbc/delete! db-spec
                :users
                ["username = ?" "theotherhodor"]))

(defn- without-registered-user [f]
  (f)
  (delete-registered-user))

(use-fixtures :once with-user without-registered-user)

(deftest test-register-flow
  (with-firefox-headless {:path-driver path-driver} driver
    (testing "register page"
      (doto driver
        (go (route-to "/daftar"))
        (wait-visible {:tag :input :type :submit}))

      (is (not (-> driver (visible? {:fn/has-text "Username tidak boleh kosong"}))))
      (is (not (-> driver (visible? {:fn/has-text "Email mesti dalam bentuk email yang sah dan tidak boleh kosong"}))))
      (is (not (-> driver (visible? {:fn/has-text "Password tidak boleh kosong"})))))

    (testing "empty input"
      (doto driver
        (click {:tag :input :type :submit})
        (wait-visible {:tag :input :type :submit}))

      (is (-> driver (visible? {:fn/has-text "Username tidak boleh kosong"})))
      (is (-> driver (visible? {:fn/has-text "Email mesti dalam bentuk email yang sah dan tidak boleh kosong"})))
      (is (-> driver (visible? {:fn/has-text "Password tidak boleh kosong"}))))

    (testing "invalid email"
      (doto driver
        (fill {:tag :input :name :email} "myemail")
        (click {:tag :input :type :submit})
        (wait-visible {:tag :input :type :submit}))

      (is (-> driver (visible? {:fn/has-text "Email mesti dalam bentuk email yang sah dan tidak boleh kosong"}))))

    (testing "username taken"
      (doto driver
        (fill {:tag :input :name :username} "hodor")
        (fill {:tag :input :name :email} "igotavalidemail@gmail.com")
        (fill {:tag :input :name :password} "abc123")
        (click {:tag :input :type :submit})
        (wait-visible {:tag :input :type :submit}))

      (is (-> driver (visible? {:fn/has-text "Username/email sudah diambil"}))))

    (testing "email taken"
      (doto driver
        (fill {:tag :input :name :username} "theotherhodor")
        (fill {:tag :input :name :email} "burhanclj@gmail.com")
        (fill {:tag :input :name :password} "theotherpassword")
        (click {:tag :input :type :submit})
        (wait-visible {:tag :input :type :submit}))

      (is (-> driver (visible? {:fn/has-text "Username/email sudah diambil"}))))

    (testing "valid registration"
      (doto driver
        (fill {:tag :input :name :username} "theotherhodor")
        (fill {:tag :input :name :email} "burhanloey@gmail.com")
        (fill {:tag :input :name :password} "theotherpassword")
        (click {:tag :input :type :submit})
        (wait-visible {:tag :input :type :submit}))

      (is (-> driver (visible? {:fn/has-text "Anda sudah berjaya mendaftar"}))))))
