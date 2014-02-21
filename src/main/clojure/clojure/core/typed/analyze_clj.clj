(ns ^:skip-wiki clojure.core.typed.analyze-clj
  (:require [clojure.tools.analyzer :as ta]
            [clojure.tools.analyzer.jvm :as taj]
            [clojure.tools.analyzer.passes.jvm.emit-form :as emit-form]
            [clojure.tools.reader :as tr]
            [clojure.tools.reader.reader-types :as readers]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.core.typed.utils :as u]))

(alter-meta! *ns* assoc :skip-wiki true)

(defn ^:private analyze1 [form env]
  (let [a (taj/analyze form env)]
    (eval (emit-form/emit-form a))
    a))

(defn ast-for-form-in-ns
  "Returns an AST node for the form 
  analyzed in the given namespace"
  [nsym form]
  (binding [*ns* (or (find-ns nsym)
                     *ns*)]
    (analyze1 form (taj/empty-env))))

(defn ast-for-form
  "Returns an AST node for the form"
  [form]
  (analyze1 form (taj/empty-env)))

(defn ast-for-ns 
  "Returns a vector of AST nodes contained
  in the given namespace symbol nsym"
  [nsym]
  {:pre [(symbol? nsym)]}
  (u/p :analyze/ast-for-ns
   ;copied basic approach from tools.emitter.jvm
   (let [res (munge nsym)
         p    (str (str/replace res #"\." "/") ".clj")
         eof  (reify)
         p (if (.startsWith p "/") (subs p 1) p)
         pres (io/resource p)
         _ (assert pres (str "Cannot find file for " nsym ": " p))
         file (-> pres io/reader slurp)
         reader (readers/indexing-push-back-reader file)]
     (binding [*ns* (or (find-ns nsym)
                        *ns*)]
       (loop [asts []]
         (let [form (tr/read reader false eof)]
           (if (not= eof form)
             (let [a (analyze1 form (taj/empty-env))]
               (recur (conj asts a)))
             asts)))))))
