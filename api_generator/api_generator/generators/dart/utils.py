from ... import utils
from typing import List
from ...schema.modeling.entities import Declarable


def get_full_name(current: Declarable) -> str:
    if current is not None:
        return get_full_name(current.parent) + utils.capitalize_camel_case(current.name)
    else:
        return ''


def make_imports(items: List[str]) -> List[str]:
    if not items:
        return []
    return [f"import '{utils.snake_case(item)}.dart';" for item in items]


dart_keywords = ['default']


def allowed_name(name: str) -> str:
    return f'{name}_' if name in dart_keywords else name
