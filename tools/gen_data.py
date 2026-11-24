"""Writes all JSON assets and data for the High Life mod into src/main/resources."""
import json, os, shutil

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
NS = "highlife"
A = os.path.join(ROOT, "assets", NS)
D = os.path.join(ROOT, "data", NS)


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


# wipe generated data + json assets, but keep textures (made by gen_textures.py)
shutil.rmtree(os.path.join(ROOT, "data"), ignore_errors=True)
for sub in ("blockstates", "models", "lang"):
    shutil.rmtree(os.path.join(A, sub), ignore_errors=True)

# ---------------- crops ----------------
CROPS = {  # block id: (seeds, produce, model parent, texture key, max produce)
    "cannabis": ("cannabis_seeds", "cannabis_bud", "minecraft:block/cross", "cross", 3),
    "tobacco": ("tobacco_seeds", "tobacco_leaf", "minecraft:block/crop", "crop", 4),
    "coca": ("coca_seeds", "coca_leaf", "minecraft:block/cross", "cross", 4),
    "salvia": ("salvia_seeds", "salvia_leaf", "minecraft:block/crop", "crop", 3),
}
STAGE_OF_AGE = [0, 0, 1, 1, 2, 2, 2, 3]

for crop, (seeds, produce, parent, key, maxp) in CROPS.items():
    w(f"{A}/blockstates/{crop}.json", {"variants": {
        f"age={age}": {"model": f"{NS}:block/{crop}_stage{STAGE_OF_AGE[age]}"} for age in range(8)}})
    for s in range(4):
        w(f"{A}/models/block/{crop}_stage{s}.json", {
            "parent": parent, "render_type": "minecraft:cutout",
            "textures": {key: f"{NS}:block/{crop}_stage{s}"}})
    mature = {"condition": "minecraft:block_state_property", "block": f"{NS}:{crop}", "properties": {"age": "7"}}
    w(f"{D}/loot_table/blocks/{crop}.json", {
        "type": "minecraft:block",
        "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"{NS}:{produce}", "conditions": [mature],
                 "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": maxp}}]},
                {"type": "minecraft:item", "name": f"{NS}:{seeds}"}]}]},
            {"rolls": 1, "bonus_rolls": 0, "conditions": [mature], "entries": [{
                "type": "minecraft:item", "name": f"{NS}:{seeds}",
                "functions": [{"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune",
                               "formula": "minecraft:binomial_with_bonus_count",
                               "parameters": {"extra": 2, "probability": 0.5714286}}]}]}],
        "functions": [{"function": "minecraft:explosion_decay"}]})

# ---------------- magic mushroom block ----------------
w(f"{A}/blockstates/magic_mushroom.json", {"variants": {"": {"model": f"{NS}:block/magic_mushroom"}}})
w(f"{A}/models/block/magic_mushroom.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                             "textures": {"cross": f"{NS}:block/magic_mushroom"}})
w(f"{D}/loot_table/blocks/magic_mushroom.json", {"type": "minecraft:block", "pools": [{
    "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{NS}:magic_mushroom"}],
    "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

# ---------------- item models ----------------
ITEMS = ["cannabis_seeds", "cannabis_bud", "dried_cannabis", "ground_cannabis", "grinder", "joint", "lit_joint",
         "bong", "cannabutter", "pot_brownie", "mushroom_tea", "tobacco_seeds", "tobacco_leaf", "dried_tobacco",
         "coca_seeds", "coca_leaf", "dried_coca_leaf", "cocaine",
         "salvia_seeds", "salvia_leaf", "dried_salvia", "ergot", "lsd_blotter", "speed_pills", "ecstasy_pill",
         "ketamine", "nitrous_balloon", "dmt"]
HANDHELD = {"joint", "lit_joint"}
for it in ITEMS:
    w(f"{A}/models/item/{it}.json", {"parent": "minecraft:item/handheld" if it in HANDHELD else "minecraft:item/generated",
                                     "textures": {"layer0": f"{NS}:item/{it}"}})
w(f"{A}/models/item/magic_mushroom.json", {"parent": "minecraft:item/generated",
                                           "textures": {"layer0": f"{NS}:block/magic_mushroom"}})

# ---------------- recipes ----------------
def ing(i):
    return {"tag": i[1:]} if i.startswith("#") else {"item": i}

def shapeless(name, inputs, out, count=1, components=None):
    res = {"id": out, "count": count}
    if components: res["components"] = components
    w(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                  "ingredients": [ing(i) for i in inputs], "result": res})

def shaped(name, pattern, key, out, count=1):
    w(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                                  "key": {k: ing(v) for k, v in key.items()}, "result": {"id": out, "count": count}})

def cook(name, inp, out, xp=0.2):
    for kind, t in (("smelting", 200), ("smoking", 100), ("campfire_cooking", 600)):
        w(f"{D}/recipe/{name}_from_{kind}.json", {"type": f"minecraft:{kind}", "category": "misc",
                                                  "ingredient": ing(inp), "result": {"id": out},
                                                  "experience": xp, "cookingtime": t})

h = lambda n: f"{NS}:{n}"
cook("dried_cannabis", h("cannabis_bud"), h("dried_cannabis"))
cook("dried_tobacco", h("tobacco_leaf"), h("dried_tobacco"))
cook("dried_coca_leaf", h("coca_leaf"), h("dried_coca_leaf"))
shaped("grinder", ["I", "F", "I"], {"I": "minecraft:iron_ingot", "F": "minecraft:flint"}, h("grinder"))
shapeless("ground_cannabis", [h("dried_cannabis"), h("grinder")], h("ground_cannabis"), 2)
shapeless("joint_from_cigarette", ["smokebreak:cigarette", h("ground_cannabis")], h("joint"))
shapeless("joint_from_paper", ["minecraft:paper", h("ground_cannabis"), h("ground_cannabis")], h("joint"))
shaped("bong", [" G ", "GWG", "GGG"], {"G": "minecraft:glass", "W": "minecraft:water_bucket"}, h("bong"))
shapeless("cannabutter", ["minecraft:milk_bucket", h("ground_cannabis"), h("ground_cannabis")], h("cannabutter"))
shapeless("pot_brownie", [h("cannabutter"), "minecraft:cocoa_beans", "minecraft:wheat", "minecraft:sugar", "minecraft:egg"],
          h("pot_brownie"), 4)
shapeless("mushroom_tea", ["minecraft:bowl", h("magic_mushroom"), h("magic_mushroom"), h("magic_mushroom"), "minecraft:sugar"],
          h("mushroom_tea"))
shapeless("cigarettes_from_tobacco", ["minecraft:paper", h("dried_tobacco")], "smokebreak:cigarette", 3)
shapeless("cocaine", [h("dried_coca_leaf")] * 4 + ["minecraft:sugar"], h("cocaine"), 2)
shapeless("cannabis_seeds", [h("cannabis_bud")], h("cannabis_seeds"), 2)
shapeless("tobacco_seeds", [h("tobacco_leaf")], h("tobacco_seeds"), 1)
shapeless("coca_seeds", [h("coca_leaf")], h("coca_seeds"), 1)
shapeless("salvia_seeds", [h("salvia_leaf")], h("salvia_seeds"), 1)
cook("dried_salvia", h("salvia_leaf"), h("dried_salvia"))
# party drugs: fictional, game-only recipes
shapeless("lsd_blotter", ["minecraft:paper", h("ergot"), "minecraft:glowstone_dust"], h("lsd_blotter"), 4)
shapeless("speed_pills", ["minecraft:sugar", "minecraft:redstone", "minecraft:gunpowder"], h("speed_pills"), 4)
shapeless("ecstasy_pill", ["minecraft:sugar", "minecraft:glowstone_dust", "minecraft:pink_dye", "minecraft:redstone"],
          h("ecstasy_pill"), 4)
shapeless("ketamine", ["minecraft:sugar", "minecraft:snowball", "minecraft:spider_eye"], h("ketamine"), 2)
shapeless("nitrous_balloon", ["minecraft:slime_ball", "minecraft:iron_nugget", "minecraft:white_dye"], h("nitrous_balloon"), 3)
shapeless("dmt", ["minecraft:amethyst_shard", "minecraft:crimson_fungus", "minecraft:warped_fungus"], h("dmt"), 2)
# Smoke Break e-liquid
shapeless("cannabis_distillate_liquid",
          ["smokebreak:empty_liquid_bottle", h("ground_cannabis"), h("ground_cannabis"), "minecraft:honey_bottle"],
          "smokebreak:liquid_bottle", 1,
          {"smokebreak:liquid": {"liquid": f"{NS}:cannabis_distillate", "amount": 10000}})
w(f"{D}/smokebreak/liquid/cannabis_distillate.json", {"color": "#8DB43A", "residue": 1.2})

# ---------------- seed drops from grass (global loot modifiers) ----------------
GRASS = {"condition": "minecraft:any_of", "terms": [
    {"condition": "minecraft:block_state_property", "block": b}
    for b in ("minecraft:short_grass", "minecraft:tall_grass", "minecraft:fern", "minecraft:large_fern")]}
NO_SHEARS = {"condition": "minecraft:inverted", "term": {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}}

def drop_table(name, item):
    w(f"{D}/loot_table/gameplay/{name}.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": item}]}]})

def modifier(name, conditions, table):
    w(f"{D}/loot_modifiers/{name}.json", {"type": "neoforge:add_table", "conditions": conditions, "table": f"{NS}:gameplay/{table}"})

drop_table("cannabis_seeds", h("cannabis_seeds"))
drop_table("tobacco_seeds", h("tobacco_seeds"))
drop_table("coca_seeds", h("coca_seeds"))
drop_table("magic_mushroom", h("magic_mushroom"))
drop_table("salvia_seeds", h("salvia_seeds"))
drop_table("ergot", h("ergot"))
modifier("salvia_seeds_from_grass", [GRASS, NO_SHEARS, {"condition": "minecraft:random_chance", "chance": 0.03}], "salvia_seeds")
modifier("ergot_from_wheat", [
    {"condition": "minecraft:block_state_property", "block": "minecraft:wheat", "properties": {"age": "7"}},
    {"condition": "minecraft:random_chance", "chance": 0.06}], "ergot")
modifier("cannabis_seeds_from_grass", [GRASS, NO_SHEARS, {"condition": "minecraft:random_chance", "chance": 0.05}], "cannabis_seeds")
modifier("tobacco_seeds_from_grass", [GRASS, NO_SHEARS, {"condition": "minecraft:random_chance", "chance": 0.05}], "tobacco_seeds")
modifier("coca_seeds_from_jungle_grass", [GRASS, NO_SHEARS,
          {"condition": "minecraft:location_check", "predicate": {"biomes": "#minecraft:is_jungle"}},
          {"condition": "minecraft:random_chance", "chance": 0.12}], "coca_seeds")
modifier("coca_seeds_from_grass", [GRASS, NO_SHEARS, {"condition": "minecraft:random_chance", "chance": 0.008}], "coca_seeds")
modifier("magic_mushroom_from_mushrooms", [
    {"condition": "minecraft:any_of", "terms": [
        {"condition": "minecraft:block_state_property", "block": "minecraft:brown_mushroom"},
        {"condition": "minecraft:block_state_property", "block": "minecraft:red_mushroom"}]},
    {"condition": "minecraft:random_chance", "chance": 0.15}], "magic_mushroom")
w(os.path.join(ROOT, "data", "neoforge", "loot_modifiers", "global_loot_modifiers.json"), {"replace": False, "entries": [
    f"{NS}:cannabis_seeds_from_grass", f"{NS}:tobacco_seeds_from_grass", f"{NS}:coca_seeds_from_jungle_grass",
    f"{NS}:coca_seeds_from_grass", f"{NS}:magic_mushroom_from_mushrooms", f"{NS}:salvia_seeds_from_grass",
    f"{NS}:ergot_from_wheat"]})

# ---------------- worldgen: magic mushroom patches ----------------
MUSH = {"Name": f"{NS}:magic_mushroom"}
w(f"{D}/worldgen/configured_feature/magic_mushroom_patch.json", {"type": "minecraft:random_patch", "config": {
    "tries": 32, "xz_spread": 6, "y_spread": 2, "feature": {
        "feature": {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": MUSH}}},
        "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
            {"type": "minecraft:would_survive", "state": MUSH}]}}]}}})
w(f"{D}/worldgen/placed_feature/magic_mushroom_patch.json", {"feature": f"{NS}:magic_mushroom_patch", "placement": [
    {"type": "minecraft:rarity_filter", "chance": 6}, {"type": "minecraft:in_square"},
    {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"}]})
w(f"{D}/neoforge/biome_modifier/magic_mushrooms.json", {
    "type": "neoforge:add_features",
    "biomes": ["minecraft:dark_forest", "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:mushroom_fields",
               "minecraft:old_growth_spruce_taiga", "minecraft:old_growth_pine_taiga", "minecraft:taiga", "minecraft:lush_caves"],
    "features": f"{NS}:magic_mushroom_patch", "step": "vegetal_decoration"})

# ---------------- tags ----------------
w(os.path.join(ROOT, "data", "minecraft", "tags", "block", "crops.json"),
  {"replace": False, "values": [h("cannabis"), h("tobacco"), h("coca"), h("salvia")]})
w(os.path.join(ROOT, "data", "minecraft", "tags", "block", "maintains_farmland.json"),
  {"replace": False, "values": [h("cannabis"), h("tobacco"), h("coca"), h("salvia")]})
w(os.path.join(ROOT, "data", "minecraft", "tags", "block", "bee_growables.json"),
  {"replace": False, "values": [h("cannabis"), h("tobacco"), h("coca"), h("salvia")]})
w(os.path.join(ROOT, "data", "neoforge", "data_maps", "item", "compostables.json"), {"values": {
    h("cannabis_seeds"): {"chance": 0.3}, h("tobacco_seeds"): {"chance": 0.3}, h("coca_seeds"): {"chance": 0.3}, h("salvia_seeds"): {"chance": 0.3}, h("salvia_leaf"): {"chance": 0.65},
    h("cannabis_bud"): {"chance": 0.65}, h("tobacco_leaf"): {"chance": 0.65}, h("coca_leaf"): {"chance": 0.65},
    h("magic_mushroom"): {"chance": 0.65}}})

# ---------------- language ----------------
LANG = {
    "itemGroup.highlife": "High Life",
    "block.highlife.cannabis": "Cannabis Plant", "block.highlife.tobacco": "Tobacco Plant", "block.highlife.coca": "Coca Bush",
    "block.highlife.magic_mushroom": "Magic Mushroom",
    "item.highlife.cannabis_seeds": "Cannabis Seeds", "item.highlife.cannabis_bud": "Cannabis Bud",
    "item.highlife.dried_cannabis": "Dried Cannabis", "item.highlife.ground_cannabis": "Ground Cannabis",
    "item.highlife.grinder": "Grinder", "item.highlife.joint": "Joint", "item.highlife.lit_joint": "Lit Joint",
    "item.highlife.bong": "Bong", "item.highlife.cannabutter": "Cannabutter", "item.highlife.pot_brownie": "Pot Brownie",
    "item.highlife.magic_mushroom": "Magic Mushroom", "item.highlife.mushroom_tea": "Mushroom Tea",
    "item.highlife.tobacco_seeds": "Tobacco Seeds", "item.highlife.tobacco_leaf": "Tobacco Leaf",
    "item.highlife.dried_tobacco": "Dried Tobacco",
    "item.highlife.coca_seeds": "Coca Seeds", "item.highlife.coca_leaf": "Coca Leaf",
    "item.highlife.dried_coca_leaf": "Dried Coca Leaf", "item.highlife.cocaine": "Bag of Cocaine",
    "effect.highlife.high": "High", "effect.highlife.tripping": "Tripping", "effect.highlife.wired": "Wired",
    "effect.highlife.comedown": "Comedown", "effect.highlife.edible_onset": "Something Kicking In...",
    "block.highlife.salvia": "Salvia Plant",
    "item.highlife.salvia_seeds": "Salvia Seeds", "item.highlife.salvia_leaf": "Salvia Leaf",
    "item.highlife.dried_salvia": "Dried Salvia", "item.highlife.dried_salvia.desc": "Smoke it in a bong. Hold on to something.",
    "item.highlife.ergot": "Ergot",
    "item.highlife.lsd_blotter": "LSD Blotter", "item.highlife.lsd_blotter.desc": "A long, colourful trip. Everything glows.",
    "item.highlife.speed_pills": "Speed Pills", "item.highlife.speed_pills.desc": "Go fast, mine fast, no sleep. Crash later.",
    "item.highlife.ecstasy_pill": "Ecstasy Pill", "item.highlife.ecstasy_pill.desc": "Love everyone. Animals love you back. Hear the beat.",
    "item.highlife.ketamine": "Ketamine", "item.highlife.ketamine.desc": "Slow motion, floaty, far away. K-hole territory.",
    "item.highlife.nitrous_balloon": "Nitrous Balloon", "item.highlife.nitrous_balloon.desc": "25 seconds of uncontrollable giggling",
    "item.highlife.dmt": "DMT Crystals", "item.highlife.dmt.desc": "Smoke it in a bong for a short, total breakthrough",
    "effect.highlife.salvia": "Salvia Pull", "effect.highlife.speeding": "Speeding", "effect.highlife.rolling": "Rolling",
    "effect.highlife.acid_trip": "Acid Trip", "effect.highlife.k_hole": "K-Hole", "effect.highlife.giggles": "Giggles",
    "effect.highlife.breakthrough": "Breakthrough",
    "message.highlife.tuesday": "The serotonin's all gone. Welcome to Tuesday.",
    "message.highlife.cant_sleep": "You're way too wired to sleep",
    "message.highlife.munchies": "This is the best thing you've ever tasted",
    "liquid.highlife.cannabis_distillate": "Cannabis Distillate",
    "liquid.highlife.cannabis_distillate.desc": "Earthy, piney and very sticky",
    "message.highlife.need_lighter": "Hold a lighter or flint and steel in your other hand",
    "message.highlife.bong_empty": "Nothing to pack: you need Ground Cannabis, Dried Salvia or DMT",
    "message.highlife.overdose": "Your heart is pounding way too hard...",
    "tooltip.highlife.joint": "Light it with a lighter in your other hand",
    "tooltip.highlife.puffs_left": "%s puffs left - hold right-click to hit it",
    "tooltip.highlife.bong": "Smokes Ground Cannabis, Dried Salvia or DMT. Your off hand picks which.",
    "tooltip.highlife.grinder": "Grinds dried cannabis in the crafting grid",
    "tooltip.highlife.pot_brownie": "Takes 30 seconds to kick in. Then it really kicks in.",
    "tooltip.highlife.magic_mushroom": "Eat it raw... if you dare",
    "tooltip.highlife.mushroom_tea": "A much stronger trip",
    "tooltip.highlife.cocaine": "Fast, strong, and a nasty comedown. Don't take a third bump.",
}
_subs = os.path.join(os.path.dirname(__file__), "subtitles.json")
if os.path.exists(_subs):
    LANG.update(json.load(open(_subs)))
w(f"{A}/lang/en_us.json", LANG)
print("data written")

# ---------------- empty 3x3x3 structure used by the game tests ----------------
import gzip, struct
def _s(x): b = x.encode(); return struct.pack('>H', len(b)) + b
def _tag(t, name, payload): return bytes([t]) + _s(name) + payload
def _i(v): return struct.pack('>i', v)
def _lst(t, items): return bytes([t]) + _i(len(items)) + b''.join(items)
def _comp(*tags): return b''.join(tags) + b'\x00'
_nbt = _tag(10, '', _comp(
    _tag(3, 'DataVersion', _i(3955)),
    _tag(9, 'size', _lst(3, [_i(3), _i(3), _i(3)])),
    _tag(9, 'palette', _lst(10, [_comp(_tag(8, 'Name', _s('minecraft:air')))])),
    _tag(9, 'blocks', _lst(10, [])),
    _tag(9, 'entities', _lst(10, []))))
os.makedirs(f"{D}/structure", exist_ok=True)
with open(f"{D}/structure/empty.nbt", "wb") as f:
    f.write(gzip.compress(_nbt))
