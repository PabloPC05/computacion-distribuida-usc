public class Bolsa {
    long quedan; // cantidad de puntos que faltan por repartir
    long bloque; // cantidad de puntos que se reparten en cada bloque

    Bolsa(long x, long y){
        quedan = x;
        bloque = y;
    }

    public synchronized long getResto(){
        if(quedan >= bloque){
            quedan -= bloque;
            return bloque;
        } else if(0 < quedan && quedan < bloque){
            long aux = quedan;
            quedan = 0;
            return aux;
        } else {
            return 0;
        }
    }
}
