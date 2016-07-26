all:
	javac -cp $(cat listaJar.txt) $(find ./ -name *.java) -d ./br.ufes.projetoFinal/bin/
run:
	java -cp $(cat listaExecute.txt)  ProjetoFinal
