(ns defereader.core
  (:gen-class))
; shortnames are unique, that's because it'll be good when indexing reads
(defn new-read
  ([link]
   (new-read link (str (java.util.UUID/randomUUID))))
  ([link shortname]
   (new-read link shortname nil))
  ([link shortname & tags]
   {:read/link link
    :read/tags tags
    :read/shortname shortname}))

(def filename (str (System/getenv "HOME") "/.defereads"))

(def reads (atom {}))

(defn load-reads [contents filename]
  (try (swap! contents (constantly (read-string (slurp filename))))
       (catch Exception e
         (prn "Exception reading: " e)
         (spit filename {:reads {} :tags {}})
         (swap! contents (constantly {:reads {} :tags {}})))))

(defn save-reads [contents filename]
  (spit filename @contents))

(defn add-read [contents read]
  (let [{:keys [read/shortname]} read]
    (if (get-in @contents [:reads shortname])
      (println "There already is a read called: " shortname)
      (let [{:keys [reads]} (deref contents)
            n-reads (assoc reads shortname read)]
        (prn read)
        (swap! contents assoc :reads n-reads)
        (doseq [tag (:read/tags read)]
          (let [n-tag (conj (get-in @contents [:tags tag] #{}) shortname)]
            (swap! contents assoc-in [:tags tag] n-tag)))
        (println "Read " shortname " successfully added!")))))

(defn find-read [contents shortname]
  (if-let [read (get-in @contents [:reads shortname])]
    read
    (println "Read not found")))

(defn list-reads [contents]
  (map #(println (first %) (str "(" (-> % second :read/link) ")")) (:reads @contents)))

(defn find-by-tag [contents tag]
  (if-let [names (get-in @contents [:tags tag])]
    (doseq [name names] (println name))
    (println "Tag does not exist yet.")))

(defn -main [& args]
  (let [arguments args]
    (load-reads reads filename)
    (case (first arguments)
      "ls" (list-reads reads)
      "show" (find-read reads (second arguments))
      "tags" (find-by-tag reads (second arguments))
      "add" (let [r (apply new-read (rest arguments))]
              (add-read reads r)
              (save-reads reads filename)))))
