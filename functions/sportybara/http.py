def get_json(session, url, params=None, headers=None, timeout=15):
    r = session.get(url, params=params, headers=headers, timeout=timeout)
    r.raise_for_status()
    return r.json()
