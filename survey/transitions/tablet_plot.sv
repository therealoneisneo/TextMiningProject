digraph finite_state_machine {
	rankdir=LR;
	size="8,5"
	node [shape = circle];
	start -> price-pos [ label = "0.1092936803" ];
	start -> cpu-pos [ label = "0.0669144981" ];
	start -> bluetooth-pos [ label = "0.0988847584" ];
	start -> price-neg [ label = "0.0847583643" ];
	start -> bluetooth-neg [ label = "0.082527881" ];
	screen-pos -> app-pos [ label = "0.3756613757" ];
	screen-pos -> battery-pos [ label = "0.0899470899" ];
	app-pos -> price-pos [ label = "0.5138121547" ];
	price-pos -> battery-pos [ label = "0.498" ];
	battery-pos -> sound-pos [ label = "0.459375" ];
	sound-pos -> battery-pos [ label = "0.0613496933" ];
	sound-pos -> sound-pos [ label = "0.0674846626" ];
	sound-pos -> camera-pos [ label = "0.3619631902" ];
	camera-pos -> price-pos [ label = "0.0503597122" ];
	camera-pos -> battery-pos [ label = "0.071942446" ];
	camera-pos -> cpu-pos [ label = "0.3021582734" ];
	camera-pos -> bluetooth-pos [ label = "0.0575539568" ];
	camera-pos -> servic-pos [ label = "0.0503597122" ];
	cpu-pos -> price-pos [ label = "0.0506329114" ];
	cpu-pos -> camera-pos [ label = "0.0506329114" ];
	cpu-pos -> microsd-pos [ label = "0.2953586498" ];
	cpu-pos -> bluetooth-pos [ label = "0.0506329114" ];
	cpu-pos -> sound-neg [ label = "0.05907173" ];
	microsd-pos -> price-pos [ label = "0.0916030534" ];
	microsd-pos -> microsd-pos [ label = "0.0610687023" ];
	microsd-pos -> internet-pos [ label = "0.3129770992" ];
	microsd-pos -> mous-pos [ label = "0.0610687023" ];
	internet-pos -> battery-pos [ label = "0.0523809524" ];
	internet-pos -> sound-pos [ label = "0.0666666667" ];
	internet-pos -> keyboard-pos [ label = "0.4333333333" ];
	keyboard-pos -> battery-pos [ label = "0.0524691358" ];
	keyboard-pos -> bluetooth-pos [ label = "0.5308641975" ];
	bluetooth-pos -> price-pos [ label = "0.0641891892" ];
	bluetooth-pos -> battery-pos [ label = "0.1081081081" ];
	bluetooth-pos -> usb-pos [ label = "0.3209459459" ];
	bluetooth-pos -> mous-pos [ label = "0.0777027027" ];
	bluetooth-pos -> screen-neg [ label = "0.0574324324" ];
	usb-pos -> price-pos [ label = "0.0612244898" ];
	usb-pos -> sound-pos [ label = "0.0612244898" ];
	usb-pos -> gps-pos [ label = "0.3605442177" ];
	gps-pos -> price-pos [ label = "0.0529411765" ];
	gps-pos -> battery-pos [ label = "0.0529411765" ];
	gps-pos -> bluetooth-pos [ label = "0.0529411765" ];
	gps-pos -> servic-pos [ label = "0.3823529412" ];
	gps-pos -> mous-pos [ label = "0.0529411765" ];
	servic-pos -> mous-pos [ label = "0.6114457831" ];
	mous-pos -> battery-pos [ label = "0.0701754386" ];
	mous-pos -> bluetooth-pos [ label = "0.0614035088" ];
	mous-pos -> usb-pos [ label = "0.0526315789" ];
	mous-pos -> screen-neg [ label = "0.4035087719" ];
	screen-neg -> app-neg [ label = "0.3413173653" ];
	screen-neg -> battery-neg [ label = "0.0538922156" ];
	screen-neg -> sound-neg [ label = "0.0718562874" ];
	screen-neg -> usb-neg [ label = "0.1976047904" ];
	app-neg -> price-neg [ label = "0.5582417582" ];
	app-neg -> usb-neg [ label = "0.0879120879" ];
	app-neg -> mous-neg [ label = "0.0527472527" ];
	price-neg -> battery-neg [ label = "0.4458128079" ];
	price-neg -> sound-neg [ label = "0.1034482759" ];
	price-neg -> usb-neg [ label = "0.118226601" ];
	price-neg -> mous-neg [ label = "0.0517241379" ];
	battery-neg -> battery-neg [ label = "0.060952381" ];
	battery-neg -> sound-neg [ label = "0.5314285714" ];
	battery-neg -> usb-neg [ label = "0.0514285714" ];
	battery-neg -> mous-neg [ label = "0.1047619048" ];
	sound-neg -> app-neg [ label = "0.0695652174" ];
	sound-neg -> battery-neg [ label = "0.052173913" ];
	sound-neg -> sound-neg [ label = "0.0608695652" ];
	sound-neg -> camera-neg [ label = "0.3304347826" ];
	sound-neg -> usb-neg [ label = "0.1565217391" ];
	camera-neg -> app-neg [ label = "0.0515463918" ];
	camera-neg -> price-neg [ label = "0.0618556701" ];
	camera-neg -> sound-neg [ label = "0.0721649485" ];
	camera-neg -> cpu-neg [ label = "0.4329896907" ];
	camera-neg -> usb-neg [ label = "0.1030927835" ];
	camera-neg -> mous-neg [ label = "0.0515463918" ];
	cpu-neg -> price-neg [ label = "0.1037037037" ];
	cpu-neg -> battery-neg [ label = "0.0740740741" ];
	cpu-neg -> sound-neg [ label = "0.0592592593" ];
	cpu-neg -> microsd-neg [ label = "0.3037037037" ];
	cpu-neg -> usb-neg [ label = "0.1333333333" ];
	cpu-neg -> gps-neg [ label = "0.0518518519" ];
	cpu-neg -> mous-neg [ label = "0.0518518519" ];
	microsd-neg -> price-neg [ label = "0.0964912281" ];
	microsd-neg -> internet-neg [ label = "0.4298245614" ];
	microsd-neg -> keyboard-neg [ label = "0.0614035088" ];
	microsd-neg -> usb-neg [ label = "0.1052631579" ];
	microsd-neg -> gps-neg [ label = "0.0526315789" ];
	microsd-neg -> mous-neg [ label = "0.0526315789" ];
	internet-neg -> price-neg [ label = "0.056" ];
	internet-neg -> battery-neg [ label = "0.084" ];
	internet-neg -> keyboard-neg [ label = "0.464" ];
	internet-neg -> usb-neg [ label = "0.096" ];
	internet-neg -> mous-neg [ label = "0.052" ];
	keyboard-neg -> battery-neg [ label = "0.0691823899" ];
	keyboard-neg -> sound-neg [ label = "0.0628930818" ];
	keyboard-neg -> keyboard-neg [ label = "0.0754716981" ];
	keyboard-neg -> bluetooth-neg [ label = "0.3899371069" ];
	keyboard-neg -> usb-neg [ label = "0.0943396226" ];
	keyboard-neg -> mous-neg [ label = "0.0754716981" ];
	bluetooth-neg -> usb-neg [ label = "0.645631068" ];
	usb-neg -> battery-neg [ label = "0.0638297872" ];
	usb-neg -> sound-neg [ label = "0.0691489362" ];
	usb-neg -> usb-neg [ label = "0.1436170213" ];
	usb-neg -> gps-neg [ label = "0.4255319149" ];
	usb-neg -> mous-neg [ label = "0.0904255319" ];
	gps-neg -> price-neg [ label = "0.0952380952" ];
	gps-neg -> battery-neg [ label = "0.0793650794" ];
	gps-neg -> usb-neg [ label = "0.0952380952" ];
	gps-neg -> servic-neg [ label = "0.3174603175" ];
	gps-neg -> mous-neg [ label = "0.0793650794" ];
	servic-neg -> battery-neg [ label = "0.0666666667" ];
	servic-neg -> sound-neg [ label = "0.0541666667" ];
	servic-neg -> usb-neg [ label = "0.0833333333" ];
	servic-neg -> mous-neg [ label = "0.5791666667" ];
	mous-neg -> app-neg [ label = "0.0520833333" ];
	mous-neg -> sound-neg [ label = "0.0625" ];
	mous-neg -> bluetooth-neg [ label = "0.0625" ];
	mous-neg -> usb-neg [ label = "0.1875" ];
	mous-neg -> mous-neg [ label = "0.09375" ];
}