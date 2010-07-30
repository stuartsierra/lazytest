(ns lazytest.report.color)

;; Mostly stolen from Stuart Halloway's circumspec

(defn colorize?
  "Colorize output, true if system property lazytest.colorize is
  true (default)"
  [] (contains? #{"yes" "true"}
                (System/getProperty "lazytest.colorize" "true")))

(defn set-colorize
  "Set the colorize? property to true or false."
  [bool]
  (assert (instance? Boolean bool))
  (System/setProperty "lazytest.colorize" (str bool)))

(def #^{:doc "ANSI color code table"}
     color-table
     {:reset "[0m"
      :bold-on "[1m"
      :italic-on "[3m"
      :underline-on "[4m"
      :inverse-on "[7m"
      :strikethrough-on "[9m"
      :bold-off "[22m"
      :italic-off "[23m"
      :underline-off "[24m"
      :inverse-off "[27m"
      :strikethrough-off "[29m"
      :black "[30m"
      :red "[31m"
      :green "[32m"
      :yellow "[33m"
      :blue "[34m"
      :magenta "[35m"
      :cyan "[36m"
      :white "[37m"
      :default "[39m"
      :bg-black "[40m"
      :bg-red "[41m"
      :bg-green "[42m"
      :bg-yellow "[43m"
      :bg-blue "[44m"
      :bg-magenta "[45m"
      :bg-cyan "[46m"
      :bg-white "[47m"
      :bg-default "[49m"})

(defn ansi-color-str
  "Return ANSI color codes for the given sequence of colors, which are
  keywords in color-table."
  [& colors]
  (apply str (map (fn [c] (str (char 27) (color-table c))) colors)))

(defn colorize
  "Wrap string s in ANSI colors if colorize? is true."
  [s & colors]
  (if (and (colorize?) (seq s))
    (str (apply ansi-color-str colors) s (ansi-color-str :reset))
    s))
