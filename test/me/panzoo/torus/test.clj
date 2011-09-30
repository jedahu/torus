(ns me.panzoo.torus.test
  (:use
    clojure.test
    clj-webdriver.core))

(deftest torus
  (let [b (start :firefox "file:///home/jdh/proj/torus/test.html")]
    (try
      (is (re-seq #"test\.html$" (text (find-it b :title nil))))

      (-> b
        (find-it {:text "state 1"})
        click)

      (is
        (= "state 1" (text (find-it b :title nil)))
        "Handler change on link-click failed.")

      (is
        (= "one" (text (find-it b :h1 nil)))
        "Single handler content not present.")

      (back b)

      (is
        (re-seq #"test\.html$" (text (find-it b :title nil)))
        "Going back in history is broken.")

      (-> b
        (find-it {:text "state 2"})
        click)

      (is
        (= "iface 1 - handler 2" (text (find-it b :title nil)))
        "Interface handler title concatenation broken.")

      (is
        (= "Title 1suffix" (text (find-it b {:id "heading"})))
        "Interface callback failed.")

      (is
        (not (exists? (find-it b :hr nil)))
        "Handler content for interface not present.")

      (is
        (= "swapped" (text (find-it b :h3 nil)))
        "Handler content for interface not present.")

      (-> b
        (find-it {:text "state 3"})
        click)

      (is
        (re-seq #"test\.html$" (text (find-it b :title nil)))
        "Default title not set.")

      (is
        (= "Title 2" (text (find-it b :h1 nil)))
        "Interface content not present.")

      (is
        (find-it b {:id "content1"})
        "Nested response 1 broken.")

      (is
        (find-it b {:id "content2"})
        "Nested response 2 broken.")

      (is
        (= "second content" (text (find-it b {:id "content2"})))
        "Wrong response 2 content.")

      (-> b
        (find-it {:text "second content"})
        click)

      (is
        (find-it b {:id "content1"})
        "Nested response 1 remains.")

      (is
        (= "home" (text (find-it b {:id "content2"})))
        "Wrong response 2 content.")

      (back b)

      (is
        (find-it b {:id "content1"})
        "Nested response 1 broken.")

      (is
        (find-it b {:id "content2"})
        "Nested response 2 broken.")

      (is
        (= "second content" (text (find-it b {:id "content2"})))
        "Wrong response 2 content.")

      (back b)

      (is
        (= "Title 1suffix" (text (find-it b {:id "heading"})))
        "Going back broke.")

      (finally
        (close b)))))
