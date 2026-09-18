@AGENTS.md

После любого изменения, добавления или рефакторинга файлов строго соблюдай следующий чеклист перед тем, как рапортовать о завершении задачи:

1. **Контрольная перепроверка отредактированного файла:**
   - Перечитай измененный файл целиком и убедись, что синтаксис корректен.
   - Проверь целостность структуры: все фигурные скобки `{ }`, круглые скобки `( )` и вызовы лямбд Compose должны быть закрыты.
   - Убедись, что не было случайно затерто содержимое соседних методов или классов при вставке дифф-патча.
   - Перепроверяй себя перед коммитом.

2. **Проверка импортов и зависимостей:**
   - Добавь все недостающие `import` (особенно для Compose-модификаторов, аннотаций, компонентов Material 3 и методов расширения Koin).

3. **Совместимость типов и сигнатур:**
   - Проверь соответствие типов передаваемых параметров сигнатурам вызываемых функций.
   - Убедись, что версии Compose/Material3 API совпадают с используемыми в проекте (не использовать устаревшие или несуществующие перегрузки функций).
   - Проверь видимость модификаторов (`private`, `internal`, `public`), чтобы не сломать доступ из других модулей.

4. **Предотвращение сбоев компиляции:**
   - Критически оцени, скомпилируется ли код на этапе `gradle assemble / build`. 
   - Если есть сомнения в наличии нужного класса или метода в зависимостях, сверься с `build.gradle.kts` перед завершением ответа.

5. **Просмотр URL:**
   - ВСЕГДА, АБСОЛЮТНО ВСЕГДА ЧИТАЙ URL КОТОРЫЕ Я ТЕБЕ КИДАЮ НА НАПРИМЕР ДОКУМЕНТАЦИЮ, НЕ ВАЖНО ЧТО У ТЕБЯ В БАЗЕ ДАННЫХ, ТЫ ДОЛЖЕН ЗНАТЬ АКТУАЛЬНУЮ ИНФОРМАЦИЮ!
   

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

Tradeoff: These guidelines bias toward caution over speed. For trivial tasks, use judgment.

1. Think Before Coding
Don't assume. Don't hide confusion. Surface tradeoffs.

Before implementing:

State your assumptions explicitly. If uncertain, ask.
If multiple interpretations exist, present them - don't pick silently.
If a simpler approach exists, say so. Push back when warranted.
If something is unclear, stop. Name what's confusing. Ask.
2. Simplicity First
Minimum code that solves the problem. Nothing speculative.

No features beyond what was asked.
No abstractions for single-use code.
No "flexibility" or "configurability" that wasn't requested.
No error handling for impossible scenarios.
If you write 200 lines and it could be 50, rewrite it.
Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

3. Surgical Changes
Touch only what you must. Clean up only your own mess.

When editing existing code:

Don't "improve" adjacent code, comments, or formatting.
Don't refactor things that aren't broken.
Match existing style, even if you'd do it differently.
If you notice unrelated dead code, mention it - don't delete it.
When your changes create orphans:

Remove imports/variables/functions that YOUR changes made unused.
Don't remove pre-existing dead code unless asked.
The test: Every changed line should trace directly to the user's request.

4. Goal-Driven Execution
Define success criteria. Loop until verified.

Transform tasks into verifiable goals:

"Add validation" → "Write tests for invalid inputs, then make them pass"
"Fix the bug" → "Write a test that reproduces it, then make it pass"
"Refactor X" → "Ensure tests pass before and after"
For multi-step tasks, state a brief plan:

1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

These guidelines are working if: fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.