import tensorflow as tf
import numpy as np
from collections import Counter


class PackDataset:

    def __init__(self, config, train_files = None, test_files = None, placeholders = None, packs = None):
        self.config = config
        self.mapping = {}
        self.entities_cnt = 0
        if not packs:
            print('Loading train...')
            self.train_entities, self.train_packs = self.read_files(train_files, shuffle=True)
            print('Loading test...')
            self.test_entities, self.test_packs = self.read_files(test_files)
        else:
            self.train_entities = []
            self.train_packs = []
            self.test_entities = []
            self.test_packs = []
            for entity, pack in packs:
                self.train_entities.append(entity)
                self.train_packs.append(pack)

        self.entities_placeholder, self.packs_before_placeholder, self.packs_after_placeholder = placeholders
        self.train_generator = None
        self.test_generator = None
        self.train_examples = len(self.train_entities)
        self.test_examples = len(self.test_entities)

    def read_files(self, files, shuffle=False):
        counter = Counter()
        entities = []
        packs = []
        for i, filename in enumerate(files):
            with open(filename, 'r') as fin:
                for line in fin:
                    items = list(map(int, line.split(',')))
                    entity = items[0]
                    if entity not in self.mapping:
                        self.entities_cnt += 1
                        self.mapping[entity] = self.entities_cnt
                    entities.append(self.mapping[entity])
                    packs.append(items[1:])
                    counter[self.mapping[entity]] += 1

        entities = np.array(entities, dtype=np.int32)
        packs = np.array(packs, dtype=np.int32)
        if shuffle:
            perm = np.random.permutation(len(entities))
            entities = entities[perm]
            packs = packs[perm]
        print(counter)
        return entities, packs

    @staticmethod
    def _generator(packs, entities, batch_size):
        assert len(packs) == len(entities)
        size = len(packs)
        for start in range(0, size, batch_size):
            end = min(start + batch_size, size)
            yield packs[start:end], entities[start:end]

    def init_epoch(self):
        self.train_generator = self._generator(self.train_packs, self.train_entities, self.config.BATCH_SIZE)
        self.test_generator = self._generator(self.test_packs, self.test_entities, self.config.TEST_BATCH_SIZE)
