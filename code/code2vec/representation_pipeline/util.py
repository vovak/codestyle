import os

import pandas as pd


class ProcessedFolder:
    def __init__(self, folder):
        self.folder = folder
        self.change_metadata_file = os.path.join(folder, "change_metadata.csv")
        self.method_ids_file = os.path.join(folder, "method_ids.csv")
        self.node_types_file = os.path.join(folder, "node_types.csv")
        self.path_ids_file = os.path.join(folder, "path_ids.csv")
        self.tokens_file = os.path.join(folder, "tokens.csv")
        self.entity_dict = os.path.join(folder, "entity_dict.pkl")
        self.reversed_entity_dict = os.path.join(folder, "reversed_entity_dict.pkl")
        self.resolved_entities = os.path.join(folder, "resolved_entities.csv")
        self.author_occurrences = os.path.join(folder, "author_occurrences.pkl")
        self.change_occurrences = os.path.join(folder, "change_occurrences.pkl")
        self.author_to_changes = os.path.join(folder, "author_to_changes.pkl")
        self.file_changes = [os.path.join(folder, f) for f in os.listdir(folder) if f.startswith("file_changes")]
        self._time_buckets_split = "time_buckets_split_{}.pkl"
        self._entity_packs = "entity_packs_{}.pkl"
        self._n_tokens = None
        self._n_paths = None
        self._trained_model_folder = "trained_model_{}_packs_{}_samples"
        self._vectorization_file = "vectorization_{}_packs_{}_samples.csv"

    def time_buckets_split(self, n_buckets):
        return os.path.join(self.folder, self._time_buckets_split.format(n_buckets))

    def entity_packs(self, pack_size):
        return os.path.join(self.folder, self._entity_packs.format(pack_size))

    def n_tokens(self):
        if self._n_tokens is None:
            tokens = pd.read_csv(self.tokens_file, index_col=0)
            self._n_tokens = len(tokens)
        return self._n_tokens

    def n_paths(self):
        if self._n_paths is None:
            paths = pd.read_csv(self.path_ids_file, index_col=0)
            self._n_paths = len(paths)
        return self._n_paths

    def trained_model_folder(self, pack_size: int, min_samples: int):
        folder = os.path.join(self.folder, self._trained_model_folder.format(pack_size, min_samples))
        if not os.path.exists(folder):
            os.mkdir(folder)
        return folder

    def vectorization_file(self, pack_size: int, min_samples: int):
        return os.path.join(self.folder, self._vectorization_file.format(pack_size, min_samples))