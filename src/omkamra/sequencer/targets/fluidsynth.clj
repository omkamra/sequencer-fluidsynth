(ns omkamra.sequencer.targets.fluidsynth
  (:require
   [clojure.java.io :as jio]
   [omkamra.sequencer :as sequencer]
   [omkamra.sequencer.targets.midi :as midi]
   [omkamra.sequencer.protocols.Target :as Target]
   [omkamra.sequencer.protocols.TargetFactory :as TargetFactory]
   [omkamra.sequencer.protocols.MidiDevice :as MidiDevice]
   [omkamra.fluidsynth.settings :as fluid-settings]
   [omkamra.fluidsynth.synth :as fluid-synth]
   [omkamra.fluidsynth.audio-driver :as fluid-audio-driver]))

(defn load-soundfonts
  [synth soundfonts]
  (-> (for [[name {:keys [path bank-offset]}] soundfonts]
        {:name name
         :path path
         :bank-offset (or bank-offset 0)
         :sfont (and (or (.exists (jio/file path))
                         (throw (ex-info "file not found" {:path path})))
                     (let [sfont (fluid-synth/sfload synth path)]
                       (fluid-synth/set-bank-offset synth sfont bank-offset)
                       sfont))})
      doall))

(defrecord FluidSynth [config settings synth soundfonts audio-driver]
  MidiDevice/protocol

  (note-on [{:keys [synth]} channel key vel]
    (fluid-synth/noteon @synth channel key vel))

  (note-off [{:keys [synth]} channel key]
    (fluid-synth/noteoff @synth channel key))

  (cc [{:keys [synth]} channel ctrl value]
    (fluid-synth/cc @synth channel ctrl value))

  (pitch-bend [{:keys [synth]} channel value]
    (fluid-synth/pitch-bend @synth channel value))

  (program-change [{:keys [synth]} channel program]
    (fluid-synth/program-change @synth channel program))

  (bank-select [{:keys [synth]} channel bank]
    (fluid-synth/bank-select @synth channel bank))

  (all-notes-off [{:keys [synth]} channel]
    (fluid-synth/all-notes-off @synth channel))

  (all-sounds-off [{:keys [synth]} channel]
    (fluid-synth/all-sounds-off @synth channel))

  Target/protocol

  (start [this]
    (if @synth
      :already-started
      (let [fluid-synth (fluid-synth/create settings)
            fluid-soundfonts (load-soundfonts fluid-synth
                                              (:soundfonts config))
            fluid-audio-driver (fluid-audio-driver/create fluid-synth)]
        (reset! synth fluid-synth)
        (reset! soundfonts fluid-soundfonts)
        (reset! audio-driver fluid-audio-driver)
        :started)))

  (stop [this]
    (if @synth
      (do
        (fluid-audio-driver/delete @audio-driver)
        (fluid-synth/delete @synth)
        (reset! synth nil)
        (reset! soundfonts nil)
        (reset! audio-driver nil)
        :stopped)
      :already-stopped))

  (restart [this]
    (Target/stop this)
    (Target/start this))

  (get-default-bindings [this]
    midi/default-bindings)

  (compile-pattern-form [this form]
    (midi/compile-pattern-form form))

  (compile-pattern-expr [this pattern]
    (midi/compile-pattern-expr pattern))

  (compile-bind-expr [this k expr]
    (midi/compile-bind-expr k expr))

  (resolve-binding [this k v]
    (midi/resolve-binding k v)))

(def default-config
  {:fluid-settings {}
   :soundfonts {}})

(defn deep-merge
  "Recursively merges maps."
  [& maps]
  (letfn [(m [to from]
            (if (and (map? from) (not (record? from)))
              (merge-with m to from)
              from))]
    (reduce m maps)))

(defn create
  [config]
  (let [config (deep-merge default-config config)
        fluid-settings (fluid-settings/create (:fluid-settings config))]
    (map->FluidSynth
     {:config config
      :settings fluid-settings
      :synth (atom nil)
      :soundfonts (atom [])
      :audio-driver (atom nil)})))

(defonce target-factory
  (reify
    TargetFactory/protocol

    (understands-descriptor?
        [this descriptor]
        (and (vector? descriptor)
             (= :fluidsynth (first descriptor))))

    (sanitize-descriptor
        [this [_ config :as descriptor]]
        (cond (string? config)
              [:fluidsynth
               {:soundfonts
                {:default {:path config
                           :bank-offset 0}}}]
              (map? config)
              (if (:soundfonts config)
                [:fluidsynth config]
                [:fluidsynth {:soundfonts config}])
              :else (throw (ex-info "invalid descriptor" {:descriptor descriptor}))))

    (make-target
        [this [_ config :as descriptor]]
        (create config))))

(sequencer/register-target-factory target-factory)
