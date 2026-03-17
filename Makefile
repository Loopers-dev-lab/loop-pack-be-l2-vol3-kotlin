init:
	git config core.hooksPath .githooks
	chmod +x .githooks/pre-commit

docs-init:
	./scripts/bootstrap-private-docs.sh
