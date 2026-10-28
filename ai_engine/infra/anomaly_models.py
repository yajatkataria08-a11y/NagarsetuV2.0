"""LSTM autoencoder (PyTorch, optional import) shared by training and inference."""
from __future__ import annotations

import numpy as np

SEQ_LEN = 64
HIDDEN = 32


def torch_available() -> bool:
    try:
        import torch  # noqa: F401
        return True
    except Exception:
        return False


def to_sequence(window: np.ndarray, seq_len: int = SEQ_LEN) -> np.ndarray:
    """Decimate a window to exactly seq_len steps (block mean, i.e. anti-aliased)."""
    n = (len(window) // seq_len) * seq_len
    return np.asarray(window[:n], dtype=np.float32).reshape(seq_len, -1).mean(axis=1)


def build_lstm_ae():
    import torch.nn as nn

    class LSTMAE(nn.Module):
        def __init__(self, hidden: int = HIDDEN):
            super().__init__()
            self.enc = nn.LSTM(1, hidden, batch_first=True)
            self.dec = nn.LSTM(hidden, hidden, batch_first=True)
            self.out = nn.Linear(hidden, 1)

        def forward(self, x):                      # x: (B, T, 1)
            _, (h, _) = self.enc(x)                # h: (1, B, H) bottleneck
            z = h[-1].unsqueeze(1).repeat(1, x.shape[1], 1)
            y, _ = self.dec(z)
            return self.out(y)

    return LSTMAE()


def reconstruction_errors(model, seqs: np.ndarray, mu: float, sd: float) -> np.ndarray:
    """Mean squared reconstruction error per sequence, using healthy mu/sd scaling."""
    import torch

    model.eval()
    x = torch.from_numpy(((seqs - mu) / sd).astype(np.float32)).unsqueeze(-1)
    with torch.no_grad():
        return ((model(x) - x) ** 2).mean(dim=(1, 2)).numpy()
