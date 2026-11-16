import math

import numpy as np
from scipy.optimize import minimize


def _sigmoid(x):
    return 1 / (1 + np.exp(-x))


class EloModel:

    def __init__(self, k: float = 20.0, home_adv: float = 75.0, base: float = 1500.0, goal_diff: bool = True):
        self.k, self.home_adv, self.base, self.goal_diff = k, home_adv, base, goal_diff
        self.ratings = {}
        self.scale, self.c = math.log(10), 0.6
        self.history = []

    def rating(self, team: str) -> float:
        return self.ratings.get(team, self.base)

    def diff(self, home: str, away: str) -> float:
        return (self.rating(home) + self.home_adv - self.rating(away)) / 400

    def expected(self, home: str, away: str) -> float:
        return 1 / (1 + 10 ** (-self.diff(home, away)))

    def update(self, home: str, away: str, hg: int, ag: int):
        d = self.diff(home, away)
        self.history.append((d, 0 if hg > ag else 1 if hg == ag else 2))
        s = 1.0 if hg > ag else 0.5 if hg == ag else 0.0
        mult = 1.0
        if self.goal_diff and abs(hg - ag) > 1:
            mult = 1 + math.log(abs(hg - ag))
        delta = self.k * mult * (s - self.expected(home, away))
        self.ratings[home] = self.rating(home) + delta
        self.ratings[away] = self.rating(away) - delta

    @staticmethod
    def _probs(d, scale, c):
        ph = _sigmoid(scale * d - c)
        pa = _sigmoid(-scale * d - c)
        return ph, 1 - ph - pa, pa

    def predict(self, home: str, away: str) -> dict:
        ph, pd, pa = self._probs(self.diff(home, away), self.scale, self.c)
        return {"home": float(ph), "draw": float(pd), "away": float(pa)}

    def calibrate(self, min_matches: int = 200):
        if len(self.history) < min_matches:
            return
        d = np.array([h[0] for h in self.history])
        y = np.array([h[1] for h in self.history])

        def nll(p):
            scale, c = p
            if scale <= 0 or c <= 0:
                return 1e9
            pr = np.stack(self._probs(d, scale, c), axis=1)
            return -np.mean(np.log(np.maximum(pr[np.arange(len(y)), y], 1e-12)))

        res = minimize(nll, [self.scale, self.c], method="Nelder-Mead")
        if res.x[0] > 0 and res.x[1] > 0:
            self.scale, self.c = float(res.x[0]), float(res.x[1])
