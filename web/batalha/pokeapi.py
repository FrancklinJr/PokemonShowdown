import requests
import random

POKEAPI_BASE = 'https://pokeapi.co/api/v2'
POKEMON_POOL = list(range(1, 152))
TIPOS_SUPORTADOS = {'normal', 'fire', 'water', 'grass', 'electric', 'psychic', 'ghost'}

def tipo_para_nosso(tipo_api):
    tipo = tipo_api.lower()
    return tipo.upper() if tipo in TIPOS_SUPORTADOS else 'NORMAL'

def buscar_move(url):
    try:
        response = requests.get(url, timeout=5)
        data = response.json()

        poder = data.get('power')
        if not poder or poder < 30:
            return None

        tipo = tipo_para_nosso(data['type']['name'])
        nome = data['name'].replace('-', ' ').title()
        return {'nome': nome, 'poder': poder, 'tipo': tipo}
    except:
        return None

def buscar_pokemon(pokemon_id):
    url = f'{POKEAPI_BASE}/pokemon/{pokemon_id}'
    response = requests.get(url, timeout=5)
    data = response.json()

    nome = data['name'].capitalize()
    sprite = data['sprites']['back_default'] or ''

    stats = {s['stat']['name']: s['base_stat'] for s in data['stats']}
    hp      = stats.get('hp', 45)
    atk     = stats.get('attack', 50)
    defense = stats.get('defense', 50)
    speed   = stats.get('speed', 50)

    tipo = tipo_para_nosso(data['types'][0]['type']['name'])

    moves_disponiveis = data['moves'].copy()
    random.shuffle(moves_disponiveis)

    moves = []

    for m in moves_disponiveis:
        if len(moves) >= 4:
            break
        move_data = buscar_move(m['move']['url'])
        if move_data:
            moves.append(move_data)

    fallbacks = [
        {'nome': 'Tackle', 'poder': 40, 'tipo': 'NORMAL'},
        {'nome': 'Quick Attack', 'poder': 40, 'tipo': 'NORMAL'},
        {'nome': 'Hyper Beam', 'poder': 150, 'tipo': 'NORMAL'},
        {'nome': 'Body Slam', 'poder': 85, 'tipo': 'NORMAL'},
    ]
    while len(moves) < 4:
        moves.append(fallbacks[len(moves) % len(fallbacks)])

    return {
        'nome': nome,
        'hp': hp,
        'atk': atk,
        'def': defense,
        'spd': speed,
        'tipo': tipo,
        'sprite': sprite,
        'moves': moves
    }

def buscar_time_aleatorio():
    ids = random.sample(POKEMON_POOL, 3)
    team = []
    for pid in ids:
        try:
            pokemon = buscar_pokemon(pid)
            team.append(pokemon)
            print(f"Buscado: {pokemon['nome']} com moves: {[m['nome'] for m in pokemon['moves']]}")
        except Exception as e:
            print(f"Erro ao buscar pokémon {pid}: {e}")
    return team