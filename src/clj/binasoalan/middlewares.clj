(ns binasoalan.middlewares
  (:require [buddy.auth.accessrules :refer [compile-access-rules success?
                                            handle-error error]]
            [buddy.auth.middleware :refer [authentication-request
                                           authorization-error]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; wrap-access-rules
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- match-access-rules
  "Copy pasted match-access-rules from buddy since it is private."
  [accessrules request]
  (reduce (fn [acc accessrule]
            (let [matcher (:matcher accessrule)
                  match-result (matcher request)]
              (when match-result
                (reduced (assoc accessrule :match-params match-result)))))
          nil
          accessrules))

(defn- apply-matched-access-rule
  "Copy pasted apply-matched-access-rule from buddy since it is private."
  [match request]
  {:pre [(map? match)
         (contains? match :handler)]}
  (let [handler (:handler match)
        params  (:match-params match)]
    (-> request
        (assoc :match-params params)
        (handler))))

(defn- handle-error-async
  [response request rule respond raise]
  (try
    (let [err (handle-error response request rule)]
      (respond err))
    (catch Exception e
      (raise e))))

(defn wrap-access-rules
  "Same with buddy's wrap-access-rules except for async handler.

  TODO: Use buddy's async middleware once they implemented it."
  [handler & [{:keys [policy rules] :or {policy :allow} :as opts}]]
  (when (nil? rules)
    (throw (IllegalArgumentException. "rules should not be empty.")))
  (let [accessrules (compile-access-rules rules)]
    (fn [request respond raise]
      (if-let [match (match-access-rules accessrules request)]
        (let [res (apply-matched-access-rule match request)]
          (if (success? res)
            (handler request respond raise)
            (handle-error-async res request (merge opts match) respond raise)))
        (case policy
          :allow (handler request respond raise)
          :reject (handle-error-async (error nil) request opts respond raise))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; wrap-authentication
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-authentication
  "Same with buddy's wrap-authentication except for async handler.

  TODO: Use buddy's async middleware once they implemented it."
  [handler & backends]
  (fn [request respond raise]
    (handler (apply authentication-request request backends) respond raise)))
