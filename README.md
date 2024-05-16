# Prompt engineering with Spring AI - Source code reviewer app

This project is aiming to provide feedback on common code interview specific problem solutions.

## Getting Started
- use your gpt api key by setting OPENAI_KEY environment variable
- start the postgres in the docker compose file
- uncomment the init line in ServiceApplication.java to initialize the vector database with the book
- comment the init line again, to avoid reinitializing the database every time the app starts
- change the resource paths to change prompts / problems / solutions

## Intended usage
I initally wanted to use it within this POC:
https://github.com/kovbe11/prompt-engineering-course

But I decided to focus on the prompt engineering part of the project.
It was easier to provide the screenshots for documentation from here,
and hooking up the docker run part with this is trivial as they are completely separate.

## Credit goes to the following resources:
- https://github.com/spring-tips/llm-rag-with-spring-ai (setup code)
- https://www.haio.ir/app/uploads/2021/12/Cracking-the-Coding-Interview-189-Programming-Questions-and-Solutions-by-Gayle-Laakmann-McDowell-z-lib.org_.pdf (book)
